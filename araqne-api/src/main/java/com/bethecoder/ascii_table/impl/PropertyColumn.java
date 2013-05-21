package com.bethecoder.ascii_table.impl;

public class PropertyColumn {
	private String property;
	private String title;

	public PropertyColumn(String property, String title) {
		this.property = property;
		this.title = title;
	}
	
	public PropertyColumn(String property) {
		this.property = property;
		this.title = property;
	}
	
	public String getProperty() {
		return property;
	}
	
	public String getTitle() {
		return title;
	}
}
