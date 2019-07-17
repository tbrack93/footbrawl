package com.project.footbrawl.entity;

// just data that will be stored in database
public class Game {
	
	public static int idCounter;
	
	private int id;
	private Team team1;
	private Team team2;
	private int team1Score;
	private int team2Score;
	
	private String status;
	
	public Game() {
		
	}

	public Game(Team team1, Team team2) {
		this.id = ++idCounter;
		this.team1 = team1;
		this.team2 = team2;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Team getTeam1() {
		return team1;
	}

	public void setTeam1(Team team1) {
		this.team1 = team1;
	}

	public Team getTeam2() {
		return team2;
	}

	public void setTeam2(Team team2) {
		this.team2 = team2;
	}

	public int getTeam1Score() {
		return team1Score;
	}

	public void setTeam1Score(int team1Score) {
		this.team1Score = team1Score;
	}

	public int getTeam2Score() {
		return team2Score;
	}

	public void setTeam2Score(int team2Score) {
		this.team2Score = team2Score;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}