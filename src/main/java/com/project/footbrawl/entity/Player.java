package com.project.footbrawl.entity;

import java.util.ArrayList;
import java.util.List;

public class Player {
	
	private int id;
	private String name;
	private int MA;
	private int ST;
	private int AG;
	private int AV;
	private int cost;
	private List<Skill> skills;
	private int team;
	
	public Player() {
		skills = new ArrayList<>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMA() {
		return MA;
	}

	public void setMA(int mA) {
		MA = mA;
	}

	public int getST() {
		return ST;
	}

	public void setST(int sT) {
		ST = sT;
	}

	public int getAG() {
		return AG;
	}

	public void setAG(int aG) {
		AG = aG;
	}

	public int getAV() {
		return AV;
	}

	public void setAV(int aV) {
		AV = aV;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public List<Skill> getSkills() {
		return skills;
	}

	public void setSkills(List<Skill> skills) {
		this.skills = skills;
	}

	public int getTeam() {
		return team;
	}

	public void setTeam(int team) {
		this.team = team;
	}
	
	public boolean hasSkill(String name) {
		for(Skill s : skills) {
			if(s.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

}
