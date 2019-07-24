package com.project.footbrawl.instance;

import java.util.List;

public class MessageFromClient extends Message {

	private List<int[]> route;
	private List<int[]> waypoints;
	private int diceChoice;
	private boolean useSkill;
	private String skillName;
	private String rerollChoice;
	
	public MessageFromClient() {
		
	}

	public List<int[]> getRoute() {
		return route;
	}

	public void setRoute(List<int[]> route) {
		this.route = route;
	}


	public int getDiceChoice() {
		return diceChoice;
	}


	public void setDiceChoice(int diceChoice) {
		this.diceChoice = diceChoice;
	}

	public boolean isUseSkill() {
		return useSkill;
	}

	public void setUseSkill(boolean useSkill) {
		this.useSkill = useSkill;
	}

	public String getSkillName() {
		return skillName;
	}

	public void setSkillName(String skillName) {
		this.skillName = skillName;
	}

	public List<int[]> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(List<int[]> waypoints) {
		this.waypoints = waypoints;
	}

	public String getRerollChoice() {
		return rerollChoice;
	}

	public void setRerollChoice(String rerollChoice) {
		this.rerollChoice = rerollChoice;
	}
	
}
