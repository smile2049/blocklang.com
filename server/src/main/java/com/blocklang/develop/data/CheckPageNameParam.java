package com.blocklang.develop.data;

import com.blocklang.develop.constant.AppType;

public class CheckPageNameParam {

	private String name;
	private Integer parentId;
	private String appType;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getParentId() {
		return parentId;
	}
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}
	public AppType getAppType() {
		return AppType.fromKey(appType);
	}
	public void setAppType(String appType) {
		this.appType = appType;
	}
	
}
