package com.project.footbrawl.instance;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

	private String type;
	private String action;
	private int player;
	private int [] location;
	private int[] target;
	private String description;
	private Integer routeMACost;
	private String rerollChoice;
	
	public Message() {
		
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}

	public int[] getTarget() {
		return target;
	}

	public void setTarget(int[] target) {
		this.target = target;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int[] getLocation() {
		return location;
	}

	public void setLocation(int[] location) {
		this.location = location;
	}

	public Integer getRouteMACost() {
		return routeMACost;
	}

	public void setRouteMACost(Integer routeMACost) {
		this.routeMACost = routeMACost;
	}
	
	public String getRerollChoice() {
		return rerollChoice;
	}

	public void setRerollChoice(String rerollChoice) {
		this.rerollChoice = rerollChoice;
	}
	

}
