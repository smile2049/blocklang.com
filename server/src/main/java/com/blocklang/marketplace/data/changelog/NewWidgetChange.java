package com.blocklang.marketplace.data.changelog;

import java.util.List;

public class NewWidgetChange implements Change {

	private String name;
	private String label;
	private String description;
	private String iconClass;
	private List<String> appType;
	private List<WidgetProperty> properties;
	private List<WidgetEvent> events;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIconClass() {
		return iconClass;
	}

	public void setIconClass(String iconClass) {
		this.iconClass = iconClass;
	}

	public List<String> getAppType() {
		return appType;
	}

	public void setAppType(List<String> appType) {
		this.appType = appType;
	}

	public List<WidgetProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<WidgetProperty> properties) {
		this.properties = properties;
	}

	public List<WidgetEvent> getEvents() {
		return events;
	}

	public void setEvents(List<WidgetEvent> events) {
		this.events = events;
	}

}
