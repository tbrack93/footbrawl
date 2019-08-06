package com.project.footbrawl.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.footbrawl.entity.Player;
import com.project.footbrawl.entity.Team;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamInGame {
	
	@JsonIgnore
	private Team team; // related entity
	@JsonIgnore
	private List<PlayerInGame> allPlayers;
	private List<PlayerInGame> reserves;
	private Set<PlayerInGame> playersOnPitch;
	private List<PlayerInGame> dugout; // KO'd
	private List<PlayerInGame> injured; // injured & dead
	private List<PlayerInGame> dungeon; // sent off for fouling
	private int turn;
	private int remainingTeamRerolls;
	// action limits (once per turn)
	private boolean rerolled;
	private boolean blitzed;
	private boolean passed;
	private boolean handedOff;
	private boolean fouled;
	private String inducements; // need to replace with real inducements
	
	public TeamInGame(Team team) {
		this.team = team;
		turn = 0;
		remainingTeamRerolls = team.getTeamRerolls();
		allPlayers = new ArrayList<>();
		reserves = new ArrayList<>();
		playersOnPitch = new HashSet<>();
		injured = new ArrayList<>();
		dugout = new ArrayList<>();
		dungeon = new ArrayList<>();
	    for(Player p : team.getPlayers()) {
	    	PlayerInGame pg = new PlayerInGame(p, this);
	    	allPlayers.add(pg);
	    	reserves.add(pg); // should start as reserves, but on pitch for testing
	    }
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public List<PlayerInGame> getReserves() {
		return reserves;
	}

	public void setReserves(List<PlayerInGame> reserves) {
		this.reserves = reserves;
	}
	
	public void addToReserves(PlayerInGame player){
		reserves.add(player);
		if(player.getTile() != null) { 
			player.getTile().removePlayer();
		}
		playersOnPitch.remove(player);
	}

	public Set<PlayerInGame> getPlayersOnPitch() {
		return playersOnPitch;
	}

	public void setPlayersOnPitch(Set<PlayerInGame> playersOnPitch) {
		this.playersOnPitch = playersOnPitch;
	}
	
	public void addPlayerOnPitch(PlayerInGame player) {
		playersOnPitch.add(player);
		reserves.remove(player);
	}
	
	public void removePlayerFromPitch(PlayerInGame player) {
		playersOnPitch.remove(player);
	}
	
	public void endTurn() {
		for(PlayerInGame p : playersOnPitch) {
			p.endTurn();
		}
	}
	
	public void newTurn() {
		resetPlayersOnPitch();
		rerolled = false;
		fouled = false;
		blitzed = false;
		passed = false;
		handedOff = false;
	}
	
	public void resetPlayersOnPitch() {
		for(PlayerInGame p : playersOnPitch) {
			p.newTurn();
		}
	}

	public List<PlayerInGame> getDugout() {
		return dugout;
	}

	public void setDugout(List<PlayerInGame> dugout) {
		this.dugout = dugout;
	}
	
	public void addToDugout(PlayerInGame player) {
		dugout.add(player);
		playersOnPitch.remove(player);
	}
	
	public void removeFromDugout(PlayerInGame player) {
		dugout.remove(player);
	}

	public int getTurn() {
		return turn;
	}

	public void setTurn(int turn) {
		this.turn = turn;
	}
	
	public void incrementTurn() {
		turn++;
	}

	public int getRemainingTeamRerolls() {
		return remainingTeamRerolls;
	}

	public void setTeamRerolls(int teamRerolls) {
		this.remainingTeamRerolls = teamRerolls;
	}
	
	public void useTeamReroll() {
		if(remainingTeamRerolls == 0) {
			throw new IllegalArgumentException("No rerolls to use");
		}
		remainingTeamRerolls--;
		rerolled = true;
	}
	
	public void resetRerolls() {
		remainingTeamRerolls = team.getTeamRerolls();
	}

	public String getInducements() {
		return inducements;
	}

	public int getId() {
		return team.getId();
	}

	public List<PlayerInGame> getDungeon() {
		return dungeon;
	}

	public void setDungeon(List<PlayerInGame> dungeon) {
		this.dungeon = dungeon;
	}
	
	public void addToDungeon(PlayerInGame player) {
		dungeon.add(player);
		playersOnPitch.remove(player);
	}

	public boolean hasBlitzed() {
		return blitzed;
	}

	public void setBlitzed(boolean blitzed) {
		this.blitzed = blitzed;
	}

	public boolean hasPassed() {
		return passed;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	public boolean hasHandedOff() {
		return handedOff;
	}

	public void setHandedOff(boolean handedOff) {
		this.handedOff = handedOff;
	}

	public boolean hasFouled() {
		return fouled;
	}

	public void setFouled(boolean fouled) {
		this.fouled = fouled;
	}

	public boolean hasRerolled() {
		return rerolled;
	}

	public void setRerolled(boolean rerolled) {
		this.rerolled = rerolled;
	}

	public List<PlayerInGame> getInjured() {
		return injured;
	}

	public void setInjured(List<PlayerInGame> injured) {
		this.injured = injured;
	}
	
	public void addToInjured(PlayerInGame player) {
		injured.add(player);
		playersOnPitch.remove(player);
	}
	
	public String getName() {
		return team.getName();
	}
	
	public PlayerInGame getPlayerById(int playerId) {
		for(PlayerInGame pg : allPlayers) {
			if (pg.getId() == playerId)
				return pg;
		}
		return null;
	}
	
	public void newKickOff() {
		reserves.addAll(playersOnPitch);
		playersOnPitch.clear();
	}
}
