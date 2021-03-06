package com.blocklang.develop.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.blocklang.core.constant.Constant;
import com.blocklang.core.exception.InvalidRequestException;
import com.blocklang.core.exception.NoAuthorizationException;
import com.blocklang.core.exception.ResourceNotFoundException;
import com.blocklang.core.model.UserInfo;
import com.blocklang.develop.constant.AppType;
import com.blocklang.develop.constant.ProjectResourceType;
import com.blocklang.develop.data.AddDependenceParam;
import com.blocklang.develop.data.ProjectDependenceData;
import com.blocklang.develop.data.UpdateDependenceParam;
import com.blocklang.develop.model.Project;
import com.blocklang.develop.model.ProjectBuildProfile;
import com.blocklang.develop.model.ProjectDependence;
import com.blocklang.develop.model.ProjectResource;
import com.blocklang.develop.service.ProjectDependenceService;
import com.blocklang.develop.service.ProjectResourceService;
import com.blocklang.marketplace.model.ApiRepo;
import com.blocklang.marketplace.model.ApiRepoVersion;
import com.blocklang.marketplace.model.ComponentRepo;
import com.blocklang.marketplace.model.ComponentRepoVersion;
import com.blocklang.marketplace.service.ApiRepoService;
import com.blocklang.marketplace.service.ApiRepoVersionService;
import com.blocklang.marketplace.service.ComponentRepoService;
import com.blocklang.marketplace.service.ComponentRepoVersionService;

/**
 * 项目依赖
 * 
 * @author Zhengwei Jin
 *
 */
@RestController
public class ProjectDependenceController extends AbstractProjectController{

	private static final Logger logger = LoggerFactory.getLogger(ProjectDependenceController.class);
	
	@Autowired
	private ProjectResourceService projectResourceService;
	@Autowired
	private ProjectDependenceService projectDependenceService;
	@Autowired
	private ComponentRepoService componentRepoService;
	@Autowired
	private ComponentRepoVersionService componentRepoVersionService;
	@Autowired
	private ApiRepoService apiRepoService;
	@Autowired
	private ApiRepoVersionService apiRepoVersionService;

	/**
	 * 获取 dependence 资源信息。
	 * 
	 * 注意，这里重点是资源信息，不是依赖详情。
	 * 
	 * <p>
	 * 就如分开获取页面基本信息和页面模型一样，依赖本身也是项目资源的一种，所以此方法用于获取项目依赖这个资源的基本信息，并不获取项目依赖项列表。
	 * 
	 * FIXME：如果是获取资源的基本信息，是不是可以将获取页面的基本信息、获取依赖的基本信息和获取 readme 等资源的基本信息，放在一个方法中呢？
	 * </p>
	 * 
	 * @param principal
	 * @param owner
	 * @param projectName
	 * @return
	 */
	@GetMapping("/projects/{owner}/{projectName}/dependence")
	public ResponseEntity<Map<String, Object>> getDependence(
			Principal principal,
			@PathVariable String owner,
			@PathVariable String projectName) {
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canRead(principal, project).orElseThrow(NoAuthorizationException::new);
		
		ProjectResource resource = projectResourceService.findByKey(
				project.getId(), 
				Constant.TREE_ROOT_ID, 
				ProjectResourceType.DEPENDENCE, 
				AppType.UNKNOWN, 
				ProjectResource.DEPENDENCE_KEY).orElseThrow(ResourceNotFoundException::new);
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("resourceId", resource.getId());
		result.put("pathes", ProjectResourcePathUtil.combinePathes(Collections.singletonList(resource)));
		return ResponseEntity.ok(result);
	}

	@PostMapping("/projects/{owner}/{projectName}/dependences")
	public ResponseEntity<ProjectDependenceData> addDependence(
			Principal principal,
			@PathVariable String owner,
			@PathVariable String projectName,
			@Valid @RequestBody AddDependenceParam param,
			BindingResult bindingResult) {
		
		if(principal == null) {
			throw new NoAuthorizationException();
		}
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);

		ComponentRepo componentRepo = componentRepoService.findById(param.getComponentRepoId()).orElseThrow(ResourceNotFoundException::new);
		
		if(componentRepo.getIsIdeExtension()) {
			// 当前只支持一个默认 profile
			// 后续版本会考虑是否需要支持多个 profile
			if(projectDependenceService.devDependenceExists(
					project.getId(), 
					param.getComponentRepoId())){
				logger.error("项目已依赖该组件仓库");
				bindingResult.rejectValue("componentRepoId", "Duplicated.dependence");
				throw new InvalidRequestException(bindingResult);
			}
		} else {
			// 当前只支持一个默认 profile
			// 后续版本会考虑是否需要支持多个 profile
			if(projectDependenceService.buildDependenceExists(
					project.getId(), 
					componentRepo.getId(),
					componentRepo.getAppType(),
					// 从客户端传过来的是 profileName
					ProjectBuildProfile.DEFAULT_PROFILE_NAME)){
				logger.error("项目已依赖该组件仓库");
				bindingResult.rejectValue("componentRepoId", "Duplicated.dependence");
				throw new InvalidRequestException(bindingResult);
			}
		}
		
		UserInfo user = userService.findByLoginName(principal.getName()).orElseThrow(NoAuthorizationException::new);
		
		ProjectDependence savedProjectDependence = projectDependenceService.save(project.getId(), componentRepo, user.getId());
		
		ApiRepo apiRepo = apiRepoService.findById(componentRepo.getApiRepoId()).orElseThrow(() -> {
			logger.error("组件仓库 {0} 没有找到对应的 API 仓库", componentRepo.getName());
			return new ResourceNotFoundException();
		});
		
		ComponentRepoVersion componentRepoVersion = componentRepoVersionService.findById(savedProjectDependence.getComponentRepoVersionId()).orElseThrow(() -> {
			logger.error("组件仓库 {0} 没有找到 ID 为 {1} 版本号", componentRepo.getName(), savedProjectDependence.getComponentRepoVersionId());
			return new ResourceNotFoundException();
		});
		
		ApiRepoVersion apiRepoVersion = apiRepoVersionService.findById(componentRepoVersion.getApiRepoVersionId()).orElseThrow(() -> {
			logger.error("API 仓库 {0} 没有找到 ID 为 {1} 版本号", apiRepo.getName(), componentRepoVersion.getApiRepoVersionId());
			return new ResourceNotFoundException();
		});
		
		ProjectDependenceData result = new ProjectDependenceData(savedProjectDependence, componentRepo, componentRepoVersion, apiRepo, apiRepoVersion);
		return new ResponseEntity<ProjectDependenceData>(result, HttpStatus.CREATED);
	}
	
	@GetMapping("/projects/{owner}/{projectName}/dependences")
	public ResponseEntity<List<ProjectDependenceData>> listDependences(
			Principal principal,
			@PathVariable String owner,
			@PathVariable String projectName) {
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canRead(principal, project).orElseThrow(NoAuthorizationException::new);
		
		List<ProjectDependenceData> result = projectDependenceService.findProjectDependences(project.getId());
		return ResponseEntity.ok(result);
	}
	
	@DeleteMapping("/projects/{owner}/{projectName}/dependences/{dependenceId}")
	public ResponseEntity<?> deleteDependence(
			Principal principal,
			@PathVariable String owner,
			@PathVariable String projectName,
			@PathVariable Integer dependenceId) {
		if(principal == null) {
			throw new NoAuthorizationException();
		}
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);
		
		// FIXME: 是否有必要添加 try，如果需要，则用测试用例确认。
		try {
			projectDependenceService.delete(dependenceId);
		}catch (EmptyResultDataAccessException e) {
			logger.warn("该依赖已不存在", e);
		}
		
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping("/projects/{owner}/{projectName}/dependences/{dependenceId}")
	public ResponseEntity<ComponentRepoVersion> updateDependence(Principal principal,
			@PathVariable String owner,
			@PathVariable String projectName,
			@PathVariable Integer dependenceId,
			@RequestBody UpdateDependenceParam param) {
		if(principal == null) {
			throw new NoAuthorizationException();
		}
		Project project = projectService.find(owner, projectName).orElseThrow(ResourceNotFoundException::new);
		projectPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);
		
		UserInfo user = userService.findByLoginName(principal.getName()).orElseThrow(NoAuthorizationException::new);
		ProjectDependence dependence = projectDependenceService.findById(dependenceId).orElseThrow(ResourceNotFoundException::new);
		dependence.setComponentRepoVersionId(param.getComponentRepoVersionId());
		dependence.setLastUpdateTime(LocalDateTime.now());
		dependence.setLastUpdateUserId(user.getId());
		projectDependenceService.update(dependence);
		ComponentRepoVersion result = componentRepoVersionService.findById(param.getComponentRepoVersionId()).orElseThrow(ResourceNotFoundException::new);
		// 因为这里只是版本更新，所以只返回组件仓库的版本信息
		return new ResponseEntity<ComponentRepoVersion>(result, HttpStatus.CREATED);
	}

}
