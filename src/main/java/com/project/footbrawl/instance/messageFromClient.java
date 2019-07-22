package com.project.footbrawl.instance;

import java.util.List;

public class MessageFromClient extends Message {

	private List<int[]> route;
	private List<int[]> waypoints;
	private int diceChoice;
	private boolean useSkill;
	private String skillName;
	private boolean useReroll;
	
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

	public boolean isUseReroll() {
		return useReroll;
	}

	public void setUseReroll(boolean useReroll) {
		this.useReroll = useReroll;
	}

	public List<int[]> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(List<int[]> waypoints) {
		this.waypoints = waypoints;
	}
	
	
	
}
