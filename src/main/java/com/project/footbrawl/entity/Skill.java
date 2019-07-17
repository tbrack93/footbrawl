package com.project.footbrawl.entity;

public class Skill {
	
	private int id;
	private String name;
	private String description;
	private String context;
	
	public Skill(String name, String description, String context) {
		this.name = name;
		this.description = description;
		this.context = context;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	

}
