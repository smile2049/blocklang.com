package com.blocklang.marketplace.data.changelog;

import java.util.List;

public class WidgetProperty {

	private String name;
	private String label;
	private String value;
	private String valueType;
	private List<WidgetPropertyOption> options;

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

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public List<WidgetPropertyOption> getOptions() {
		return options;
	}

	public void setOptions(List<WidgetPropertyOption> options) {
		this.options = options;
	}

}