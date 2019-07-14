package instance;

import java.util.ArrayList;
import java.util.List;

import entity.Player;
import entity.Team;

public class TeamInGame {
	
	private Team team; // related entity
	private List<PlayerInGame> reserves;
	private List<PlayerInGame> playersOnPitch;
	private List<PlayerInGame> dugout;
	private List<PlayerInGame> dungeon;
	private int turn;
	private int teamRerolls;
	// action limits (once per turn)
	private boolean blitzed;
	private boolean passed;
	private boolean handedOff;
	private boolean fouled;
	private String inducements; // need to replace with real inducements
	
	public TeamInGame(Team team) {
		this.team = team;
		reserves = new ArrayList<>();
		playersOnPitch = new ArrayList<>();
		dugout = new ArrayList<>();
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

	public List<PlayerInGame> getPlayersOnPitch() {
		return playersOnPitch;
	}

	public void setPlayersOnPitch(List<PlayerInGame> playersOnPitch) {
		this.playersOnPitch = playersOnPitch;
	}

	public List<PlayerInGame> getDugout() {
		return dugout;
	}

	public void setDugout(List<PlayerInGame> dugout) {
		this.dugout = dugout;
	}

	public int getTurn() {
		return turn;
	}

	public void setTurn(int turn) {
		this.turn = turn;
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
	
}
