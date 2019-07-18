package com.project.footbrawl.instance;

import java.util.List;

public class MessageToClient extends Message {

	private int numberOfDice;
	private int userToChoose;
	private List<jsonTile> route; // for moving to a specific point
	private List<jsonTile> squares; // for seeing all squares player can move to
	private int rollNeeded;
	private int rolled;
	
	public MessageToClient() {
		
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

	public int getDice() {
		return numberOfDice;
	}

	public void setDice(int dice) {
		this.numberOfDice = dice;
	}

	public int getUserToChoose() {
		return userToChoose;
	}

	public void setUserToChoose(int userToChoose) {
		this.userToChoose = userToChoose;
	}

	public int getNumberOfDice() {
		return numberOfDice;
	}

	public void setNumberOfDice(int numberOfDice) {
		this.numberOfDice = numberOfDice;
	}

	public int getRollNeeded() {
		return rollNeeded;
	}

	public void setRollNeeded(int rollNeeded) {
		this.rollNeeded = rollNeeded;
	}

	public int getRolled() {
		return rolled;
	}

	public void setRolled(int rolled) {
		this.rolled = rolled;
	}
	
	
	
}
