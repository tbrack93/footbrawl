package instance;

import java.util.ArrayList;
import java.util.List;

import entity.Player;
import entity.Team;

public class TeamInGame {
	
	private Team team; // related entity
	private List<PlayerInGame> reserves;
	private List<PlayerInGame> playersOnPitch;
	private List<PlayerInGame> dugout; // KO'd
	private List<PlayerInGame> injured; // injured & dead
	private List<PlayerInGame> dungeon; // sent off for fouling
	private int turn;
	private int teamRerolls;
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
		reserves = new ArrayList<>();
		playersOnPitch = new ArrayList<>();
		injured = new ArrayList<>();
		dugout = new ArrayList<>();
		dungeon = new ArrayList<>();
	    for(Player p : team.getPlayers()) {
	    	playersOnPitch.add(new PlayerInGame(p, this)); // should start as reserves, but on pitch for testing
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
	}

	public List<PlayerInGame> getPlayersOnPitch() {
		return playersOnPitch;
	}

	public void setPlayersOnPitch(List<PlayerInGame> playersOnPitch) {
		this.playersOnPitch = playersOnPitch;
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

	public int getTeamRerolls() {
		return teamRerolls;
	}

	public void setTeamRerolls(int teamRerolls) {
		this.teamRerolls = teamRerolls;
	}

	public String getInducements() {
		return inducements;
	}

	public void setInducements(String inducements) {
		this.inducements = inducements;
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
	}
	
	public String getName() {
		return team.getName();
	}
	
}
