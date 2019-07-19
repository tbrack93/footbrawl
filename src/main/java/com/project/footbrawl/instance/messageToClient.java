package com.project.footbrawl.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageToClient extends Message {

	private Integer numberOfDice; // Integers so default to null
	private Integer userToChoose;
	private List<jsonTile> route; // for moving to a specific point
	private List<jsonTile> squares; // for seeing all squares player can move to
	private Integer rollNeeded;
	private Integer rolled;
	private String team1Name;
	private String team2Name;
	private List<PlayerInGame> team1;
	private List<PlayerInGame> team2;
	
	public MessageToClient() {
        route = null;
		squares = null;
	}

	public List<jsonTile> getSquares() {
		return squares;
	}

	public void setSquares(List<jsonTile> squares) {
		this.squares = squares;
	}

	public List<jsonTile> getRoute() {
		return route;
	}

	public void setRoute(List<jsonTile> route) {
		this.route = route;
	}

	public Integer getDice() {
		return numberOfDice;
	}

	public void setDice(int dice) {
		this.numberOfDice = dice;
	}

	public Integer getUserToChoose() {
		return userToChoose;
	}

	public void setUserToChoose(int userToChoose) {
		this.userToChoose = userToChoose;
	}

	public Integer getNumberOfDice() {
		return numberOfDice;
	}

	public void setNumberOfDice(int numberOfDice) {
		this.numberOfDice = numberOfDice;
	}

	public Integer getRollNeeded() {
		return rollNeeded;
	}

	public void setRollNeeded(int rollNeeded) {
		this.rollNeeded = rollNeeded;
	}

	public Integer getRolled() {
		return rolled;
	}

	public void setRolled(int rolled) {
		this.rolled = rolled;
	}

	public String getTeam1Name() {
		return team1Name;
	}

	public void setTeam1Name(String team1Name) {
		this.team1Name = team1Name;
	}

	public String getTeam2Name() {
		return team2Name;
	}

	public void setTeam2Name(String team2Name) {
		this.team2Name = team2Name;
	}

	public List<PlayerInGame> getTeam1() {
		return team1;
	}

	public void setTeam1(List<PlayerInGame> team1) {
		this.team1 = team1;
	}

	public List<PlayerInGame> getTeam2() {
		return team2;
	}

	public void setTeam2(List<PlayerInGame> team2) {
		this.team2 = team2;
	}
	
	
	
	
}
