package com.project.footbrawl.instance;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.footbrawl.entity.Player;
import com.project.footbrawl.entity.Skill;

public class PlayerInGame{
	
	@JsonIgnore
	private Player player;
	private int remainingMA;
	private boolean actedThisTurn;
	private boolean actionOver;
	private boolean hasTackleZones;
	private boolean hasBall;
	private String status;
	private List<String> skillsUsedThisTurn;
	@JsonIgnore
	private Tile tile;
	@JsonIgnore
	private TeamInGame teamIG;
	
	
	public PlayerInGame(Player player) {
		this.player = player;
		hasTackleZones = true;
		remainingMA = player.getMA();
		status = "standing";
		skillsUsedThisTurn = new ArrayList<>();
	}
	
	public PlayerInGame(Player player, TeamInGame tig) {
		this.player = player;
		hasTackleZones = true;
		remainingMA = player.getMA();
		status = "standing";
		teamIG = tig;
		skillsUsedThisTurn = new ArrayList<>();
	}
	
	public void newTurn() {
		remainingMA = player.getMA();
		actedThisTurn = false;
		actionOver = false;
	}
	
	public void endTurn() {
		if(status.equals("stunned")) {
			status = "prone";
		}
		actionOver = true;
	}
	
	public void setMA(int MA) {
		player.setMA(MA);;
		this.remainingMA = MA;
	}

	public int getRemainingMA() {
		return remainingMA;
	}

	public void setRemainingMA(int remainingMA) {
		this.remainingMA = remainingMA;
	}
	
	public void decrementRemainingMA() {
		this.remainingMA--;
	}

	public boolean isActedThisTurn() {
		return actedThisTurn;
	}

	public void setActedThisTurn(boolean actedThisTurn) {
		this.actedThisTurn = actedThisTurn;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		hasTackleZones = status.equals("standing");
		this.status = status;
	}
	
	public void setProne() {
		status = "prone";
		hasTackleZones = false;
		actedThisTurn = true;
		actionOver = true;
	}

	public boolean isHasTackleZones() {
		return hasTackleZones;
	}

	public void setHasTackleZones(boolean hasTackleZones) {
		this.hasTackleZones = hasTackleZones;
	}

	public Tile getTile() {
		return tile;
	}

	public void setTile(Tile tile) {
		this.tile = tile;
	}
	
	public void setHasBall(boolean ball) {
		this.hasBall = ball;
	}
	
	public boolean isHasBall() {
		return hasBall;
	}
	
	public int getId() {
		return player.getId();
	}


	public String getName() {
		return player.getName();
	}


	public int getMA() {
		return player.getMA();
	}

	public int getST() {
		return player.getST();
	}

	public int getAG() {
		return player.getAG();
	}

	public int getAV() {
		return player.getAV();
	}


	public List<Skill> getSkills() {
		return player.getSkills();
	}
	
	public boolean hasSkill(String name) {
		return player.hasSkill(name);
	}

	public int getTeam() {
		return player.getTeam();
	}

	public void setActionOver(boolean over) {
		actionOver = over;
		if(over == true) {
			remainingMA = -5;
		}
	}

	public boolean getActedThisTurn() {
		return actedThisTurn;
	}

	public boolean getActionOver() {
		return actionOver;
	}

	public TeamInGame getTeamIG() {
		return teamIG;
	}

	public void setTeamIG(TeamInGame teamIG) {
		this.teamIG = teamIG;
	}
	
	public String getImgUrl() {
		return player.getImgUrl();
	}
	
	public int[] getLocation() {
		if(tile == null) {
			return null;
		}
		return tile.getLocation();
	}

	public List<String> getSkillsUsedThisTurn() {
		return skillsUsedThisTurn;
	}
	
	public boolean hasUsedSkill(String skill) {
		if(skillsUsedThisTurn.contains(skill)) {
			return true;
		}
		return false;
	}

	public void setSkillsUsedThisTurn(List<String> skillsUsedThisTurn) {
		this.skillsUsedThisTurn = skillsUsedThisTurn;
	}
	
	public void useSkill(String skill) {
		skillsUsedThisTurn.add(skill);
	}
	
}
