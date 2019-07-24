package com.project.footbrawl.entity;

import java.util.ArrayList;
import java.util.List;

public class Team {

	public static int idCounter = 1;
	
	private int id;
	private String name;
	private List<Player> players;
	private int teamRerolls;

	public Team() {
		this.id = idCounter;
		idCounter++;
		players = new ArrayList<Player>();
	}

	public Team(String name) {
		this.id = idCounter;
		idCounter++;
		players = new ArrayList<Player>();
		this.name = name;
	}

	public int getIdCounter() {
		return idCounter;
	}

	public void setIdCounter(int idCounter) {
		this.idCounter = idCounter;
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

	public List<Player> getPlayers() {
		return players;
	}

	public void setPlayers(List<Player> players) {
		this.players = players;
	}

	public Player getPlayer(int id) {
		for (Player p : players) {
			if (p.getId() == id) {
				return p;
			}
		}
		return null;
	}

	public void addPlayer(Player p) {
		players.add(p);
	}

	public int getTeamRerolls() {
		return teamRerolls;
	}

	public void setTeamRerolls(int teamRerolls) {
		this.teamRerolls = teamRerolls;
	}

}
