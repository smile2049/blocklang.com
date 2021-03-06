package com.blocklang.develop.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.blocklang.core.constant.Constant;
import com.blocklang.core.controller.SpringMvcUtil;
import com.blocklang.core.exception.InvalidRequestException;
import com.blocklang.core.exception.NoAuthorizationException;
import com.blocklang.core.exception.ResourceNotFoundException;
import com.blocklang.core.model.UserInfo;
import com.blocklang.core.service.UserService;
import com.blocklang.develop.constant.AppType;
import com.blocklang.develop.constant.ProjectResourceType;
import com.blocklang.develop.data.CheckPageKeyParam;
import com.blocklang.develop.data.CheckPageNameParam;
import com.blocklang.develop.data.NewPageParam;
import com.blocklang.develop.model.Project;
import com.blocklang.develop.model.ProjectResource;
import com.blocklang.develop.service.ProjectResourceService;
import com.blocklang.develop.service.ProjectService;

@RestController
public class PageController extends AbstractProjectController {
	private static final Logger logger = LoggerFactory.getLogger(PageController.class);
	
	@Autowired
	private ProjectService projectService;
	@Autowired
	private ProjectResourceService projectResourceService;
	@Autowired
	private UserService userService;
	@Autowired
	private MessageSource messageSource;

	@PostMapping("/projects/{owner}/{projectName}/pages/check-key")
	public ResponseEntity<Map<String, String>> checkKey(
			Principal principal,
			@PathVariable("owner") String owner,
			@PathVariable("projectName") String projectName,
			@Valid @RequestBody CheckPageKeyParam param, 
			BindingResult bindingResult){
		
		if(principal == null) {
			throw new NoAuthorizationException();
		}
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);
		
		//校验 key: 是否为空
		if(bindingResult.hasErrors()) {
			logger.error("名称不能为空");
			throw new InvalidRequestException(bindingResult);
		}
		
		String key = param.getKey().trim();
		//校验：只支持英文字母、数字、中划线(-)、下划线(_)、点(.)
		String regEx = "^[a-zA-Z0-9\\-\\w]+$";
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(key);
		if(!matcher.matches()) {
			logger.error("包含非法字符");
			bindingResult.rejectValue("key", "NotValid.pageKey");
			throw new InvalidRequestException(bindingResult);
		}
		
		Integer parentId = param.getParentId();
		projectResourceService.findByKey(
				project.getId(), 
				parentId, 
				ProjectResourceType.PAGE, 
				param.getAppType(),
				key).map(resource -> {
			logger.error("key 已被占用");
			
			if(parentId == Constant.TREE_ROOT_ID) {
				return new Object[] {"根目录", key};
			}
			
			// 这里不需要做是否存在判断，因为肯定存在。
			ProjectResource parentResource = projectResourceService.findById(parentId).get();
			String parentResourceName = StringUtils.isBlank(parentResource.getName()) ? parentResource.getKey() : parentResource.getName();
			return new Object[] {parentResourceName, key};
		}).ifPresent(args -> {
			bindingResult.rejectValue("key", "Duplicated.pageKey", args, null);
			throw new InvalidRequestException(bindingResult);
		});
		
		return ResponseEntity.ok(new HashMap<String, String>());
	}
	
	@PostMapping("/projects/{owner}/{projectName}/pages/check-name")
	public ResponseEntity<Map<String, String>> checkName(
			Principal principal,
			@PathVariable("owner") String owner,
			@PathVariable("projectName") String projectName,
			@Valid @RequestBody CheckPageNameParam param, 
			BindingResult bindingResult){

		if(principal == null) {
			throw new NoAuthorizationException();
		}
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);
		
		// name 的值可以为空
		if(StringUtils.isNotBlank(param.getName())) {
			String name = param.getName().trim();
			
			Integer parentId = param.getParentId();
			projectResourceService.findByName(
					project.getId(), 
					parentId, 
					ProjectResourceType.PAGE, 
					param.getAppType(),
					name).map(resource -> {
				logger.error("name 已被占用");
				
				if(parentId == Constant.TREE_ROOT_ID) {
					return new Object[] {"根目录", name};
				}
				
				// 这里不需要做是否存在判断，因为肯定存在。
				ProjectResource parentResource = projectResourceService.findById(parentId).get();
				String parentResourceName = StringUtils.isBlank(parentResource.getName()) ? parentResource.getKey() : parentResource.getName();
				return new Object[] {parentResourceName, name};
			}).ifPresent(args -> {
				bindingResult.rejectValue("name", "Duplicated.pageName", args, null);
				throw new InvalidRequestException(bindingResult);
			});
		}
		return ResponseEntity.ok(new HashMap<String, String>());
	}

	@PostMapping("/projects/{owner}/{projectName}/pages")
	public ResponseEntity<ProjectResource> newPage(
			Principal principal, 
			@PathVariable("owner") String owner,
			@PathVariable("projectName") String projectName,
			@Valid @RequestBody NewPageParam param, 
			BindingResult bindingResult) {
		if(principal == null) {
			throw new NoAuthorizationException();
		}
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);
		
		//校验 key: 
		boolean keyIsValid = true;
		// 一、是否为空
		if(bindingResult.hasErrors()) {
			logger.error("名称不能为空");
			keyIsValid = false;
		}
		
		String key = Objects.toString(param.getKey(), "").trim();
		if(keyIsValid) {
			//校验：只支持英文字母、数字、中划线(-)、下划线(_)、点(.)
			String regEx = "^[a-zA-Z0-9\\-\\w]+$";
			Pattern pattern = Pattern.compile(regEx);
			Matcher matcher = pattern.matcher(key);
			if(!matcher.matches()) {
				logger.error("包含非法字符");
				bindingResult.rejectValue("key", "NotValid.pageKey");
				keyIsValid = false;
			}
		}
		
		if(keyIsValid) {
			Integer parentId = param.getParentId();
			projectResourceService.findByKey(
					project.getId(), 
					parentId, 
					ProjectResourceType.PAGE, 
					param.getAppType(),
					key).map(resource -> {
				logger.error("key 已被占用");
				
				if(parentId == Constant.TREE_ROOT_ID) {
					return new Object[] {"根目录", key};
				}
				
				// 这里不需要做是否存在判断，因为肯定存在。
				return new Object[] {projectResourceService.findById(parentId).get().getName(), key};
			}).ifPresent(args -> {
				bindingResult.rejectValue("key", "Duplicated.pageKey", args, null);
			});
		}
		
		Integer parentId = param.getParentId();
		// 校验 name
		// name 可以为空
		if(StringUtils.isNotBlank(param.getName())) {
			String name = param.getName().trim();
			projectResourceService.findByName(
					project.getId(), 
					parentId, 
					ProjectResourceType.PAGE, 
					param.getAppType(),
					name).map(resource -> {
				logger.error("name 已被占用");
				
				if(parentId == Constant.TREE_ROOT_ID) {
					return new Object[] {"根目录", name};
				}
				
				// 这里不需要做是否存在判断，因为肯定存在。
				return new Object[] {projectResourceService.findById(parentId).get().getName(), name};
			}).ifPresent(args -> {
				bindingResult.rejectValue("name", "Duplicated.pageName", args, null);
			});
			
		}
		
		if(bindingResult.hasErrors()) {
			throw new InvalidRequestException(bindingResult);
		}
		
		ProjectResource resource = new ProjectResource();
		resource.setProjectId(project.getId());
		resource.setParentId(parentId);
		resource.setAppType(param.getAppType());
		resource.setKey(key);
		resource.setName(param.getName() == null ? null : param.getName().trim());
		if(param.getDescription() != null) {
			resource.setDescription(param.getDescription().trim());
		}
		resource.setResourceType(ProjectResourceType.PAGE);
		
		UserInfo currentUser = userService.findByLoginName(principal.getName()).orElseThrow(NoAuthorizationException::new);
		resource.setCreateUserId(currentUser.getId());
		resource.setCreateTime(LocalDateTime.now());
		
		ProjectResource savedProjectResource = projectResourceService.insert(project, resource);
		savedProjectResource.setMessageSource(messageSource);
		return new ResponseEntity<ProjectResource>(savedProjectResource, HttpStatus.CREATED);
	}
	
	@GetMapping("/projects/{owner}/{projectName}/pages/**")
	public ResponseEntity<Map<String, Object>> getPage(Principal user,
			@PathVariable String owner,
			@PathVariable String projectName,
			HttpServletRequest req) {
		// 先校验用户对项目是否有读取权限
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canRead(user, project).orElseThrow(NoAuthorizationException::new);
		
		String pagePath = SpringMvcUtil.getRestUrl(req, 4);
		// 获取表示页面的 key
		String[] pathes = pagePath.split("/");
		String groupPath = StringUtils.join(pathes, "/", 0, pathes.length - 1);
		// 要校验根据 parentPath 中的所有节点都能准确匹配
		// 这个列表中不包含页面信息
		List<ProjectResource> parentGroups = projectResourceService.findParentGroupsByParentPath(project.getId(), groupPath);
		
		String pageKey = pathes[pathes.length - 1];
		Integer parentGroupId = Constant.TREE_ROOT_ID;
		if(!parentGroups.isEmpty()) {
			parentGroupId = parentGroups.get(parentGroups.size() - 1).getId();
		}
		ProjectResource projectResource = projectResourceService.findByKey(project.getId(), parentGroupId, ProjectResourceType.PAGE, AppType.WEB, pageKey).orElseThrow(ResourceNotFoundException::new);
		projectResource.setMessageSource(messageSource); // 不加这行代码，在生成 json 时会出错
		
		parentGroups.add(projectResource);
		List<Map<String, String>> stripedParentGroups = ProjectResourcePathUtil.combinePathes(parentGroups);
		
		Map<String, Object> result = new HashMap<>();
		result.put("projectResource", projectResource);
		result.put("parentGroups", stripedParentGroups);
		
		return ResponseEntity.ok(result);
	}

}
