package com.blocklang.marketplace.controller;

import java.net.URISyntaxException;
import java.security.Principal;
import java.time.LocalDateTime;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blocklang.core.exception.InvalidRequestException;
import com.blocklang.core.exception.NoAuthorizationException;
import com.blocklang.core.exception.ResourceNotFoundException;
import com.blocklang.core.git.GitUtils;
import com.blocklang.core.model.UserInfo;
import com.blocklang.core.service.UserService;
import com.blocklang.marketplace.data.NewComponentRepoParam;
import com.blocklang.marketplace.model.ComponentRepoPublishTask;
import com.blocklang.marketplace.model.ComponentRepo;
import com.blocklang.marketplace.service.ComponentRepoPublishTaskService;
import com.blocklang.marketplace.service.ComponentRepoRegistryService;
import com.blocklang.marketplace.service.PublishService;
import com.blocklang.release.constant.ReleaseResult;

@RestController
public class ComponentRepoController {
	
	@Autowired
	private UserService userService;
	@Autowired
	private ComponentRepoRegistryService componentRepoRegistryService;
	@Autowired
	private ComponentRepoPublishTaskService componentRepoPublishTaskService;
	@Autowired
	private PublishService publishService;

	@GetMapping("/component-repos")
	public ResponseEntity<Page<ComponentRepo>> listComponentRepos(
			@RequestParam(value="q", required = false)String query, 
			@RequestParam(required = false) String page) {
		Integer iPage = null;
		if(StringUtils.isBlank(page)){
			iPage = 0;
		}else {
			try {
				iPage = Integer.valueOf(page);
			}catch (NumberFormatException e) {
				throw new ResourceNotFoundException();
			}
		}
		
		if(iPage < 0) {
			throw new ResourceNotFoundException();
		}
		
		// 默认一页显示 60 项组件仓库
		Pageable pageable = PageRequest.of(iPage, 60, Sort.by(Direction.DESC, "lastPublishTime"));
		Page<ComponentRepo> result = componentRepoRegistryService.findAllByNameOrLabel(query, pageable);
		
		if(iPage > result.getTotalPages()) {
			throw new ResourceNotFoundException();
		}
		
		return ResponseEntity.ok(result);
	}
	
	@PostMapping("/component-repos")
	public ResponseEntity<ComponentRepoPublishTask> newComponentRepo(
			Principal principal,
			@Valid @RequestBody NewComponentRepoParam param, 
			BindingResult bindingResult) {
		
		if(principal == null) {
			throw new NoAuthorizationException();
		}
		
		if(bindingResult.hasErrors()) {
			throw new InvalidRequestException(bindingResult);
		}
		
		UserInfo currentUser = userService.findByLoginName(principal.getName()).orElseThrow(NoAuthorizationException::new);
		Integer currentUserId = currentUser.getId();
		
		String gitUrl = param.getGitUrl().trim();
		
		URIish uriish = null;
		try {
			uriish = new URIish(gitUrl);
		} catch (URISyntaxException e) {
			bindingResult.rejectValue("gitUrl", "NotValid.componentRepoGitUrl");
			throw new InvalidRequestException(bindingResult);
		}
		if(!uriish.isRemote()) {
			bindingResult.rejectValue("gitUrl", "NotValid.componentRepoGitUrl.invalidRemoteUrl");
			throw new InvalidRequestException(bindingResult);
		}
		
		if(!"https".equalsIgnoreCase(uriish.getScheme())) {
			bindingResult.rejectValue("gitUrl", "NotValid.componentRepoGitUrl.shouldBeHttps");
			throw new InvalidRequestException(bindingResult);
		}
		
		if(!GitUtils.isValidRemoteRepository(gitUrl)) {
			bindingResult.rejectValue("gitUrl", "NotValid.componentRepoGitUrl.repoNotExist");
			throw new InvalidRequestException(bindingResult);
		}
		
		componentRepoPublishTaskService.findByGitUrlAndUserId(currentUser.getId(), gitUrl).ifPresent(task -> {
			bindingResult.rejectValue("gitUrl", "Duplicated.componentRepoGitUrl", new Object[] {principal.getName()}, null);
			throw new InvalidRequestException(bindingResult);
		});
		
		ComponentRepoPublishTask task = new ComponentRepoPublishTask();
		task.setGitUrl(gitUrl);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(currentUserId);
		task.setCreateUserName(principal.getName());
		ComponentRepoPublishTask savedTask = componentRepoPublishTaskService.save(task);

		// 异步任务
		publishService.asyncPublish(savedTask);
		
		return new ResponseEntity<ComponentRepoPublishTask>(savedTask, HttpStatus.CREATED);
	}
	
}
