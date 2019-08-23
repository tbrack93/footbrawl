package com.project.footbrawl.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="game")
public class Game {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private int id;
	
	@Column(name="game_status")
	private String status;
	
	@Column(name="team1_score")
	private int team1Score;
	
	@Column(name="team2_score")
	private int team2Score;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade= {CascadeType.MERGE})
    @JoinColumn(name="team1_id")
	private Team team1;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade= {CascadeType.MERGE})
    @JoinColumn(name="team2_id")
	private Team team2; 
	
	public Game() {
		status = "created";
		team1Score = 0;
		team2Score = 0;
	}

	public Game(Team team1, Team team2) {
		status = "created";
		this.team1 = team1;
		this.team2 = team2;
		team1Score = 0;
		team2Score = 0;
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
	
	public void duplicateGame(Game game) {
		team1 = game.team1;
		team2 = game.team2;
	}

}
