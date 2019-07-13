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
	private int turn;
	private int teamRerolls;
	private String inducements; // need to replace with real inducements
	
	public TeamInGame(Team team) {
		this.team = team;
		reserves = new ArrayList<>();
		playersOnPitch = new ArrayList<>();
		dugout = new ArrayList<>();
	    for(Player p : team.getPlayers()) {
	    	playersOnPitch.add(new PlayerInGame(p)); // should start as reserves, but on pitch for testing
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
	
}
