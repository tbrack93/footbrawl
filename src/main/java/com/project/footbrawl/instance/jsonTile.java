package com.project.footbrawl.instance;

public class jsonTile {

	private int[] position;
	private int tackleZones;
	private int dodgeRoll;
	private int goingForItRoll;
	private int pickUpBallRoll;
	private int standUpRoll;

	public jsonTile(Tile tile) {
		this.position = tile.getPosition();
		this.tackleZones = tile.getTackleZones();
	}

	public int[] getPosition() {
		return position;
	}

	public void setPosition(int[] position) {
		this.position = position;
	}

	public int getTackleZones() {
		return tackleZones;
	}

	public void setTackleZones(int tackleZones) {
		this.tackleZones = tackleZones;
	}

	public int getDodgeRoll() {
		return dodgeRoll;
	}

	public void setDodgeRoll(int dodgeRoll) {
		this.dodgeRoll = dodgeRoll;
	}

	public int getGoingForItRoll() {
		return goingForItRoll;
	}

	public void setGoingForItRoll(int goingForItRoll) {
		this.goingForItRoll = goingForItRoll;
	}

	public int getPickUpBallRoll() {
		return pickUpBallRoll;
	}

	public void setPickUpBallRoll(int pickUpBallRoll) {
		this.pickUpBallRoll = pickUpBallRoll;
	}

	public int getStandUpRoll() {
		return standUpRoll;
	}

	public void setStandUpRoll(int standUpRoll) {
		this.standUpRoll = standUpRoll;
	}
}
