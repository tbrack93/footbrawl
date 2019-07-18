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
	
	
	
}
