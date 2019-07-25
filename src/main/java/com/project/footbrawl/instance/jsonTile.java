package com.project.footbrawl.instance;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class jsonTile {

	private int[] position; // position
	private Integer tackleZones; // tackleZones
	private Integer dodgeRoll; // dodgeRoll
	private Integer goingForItRoll; // goingForItRoll
	private Integer pickUpBallRoll; // pickUpBallRoll
	private Integer standUpRoll; // standUpRoll

	public jsonTile(Tile tile) {
		this.position = tile.getLocation();
		this.tackleZones = tile.getTackleZones();
		if(tackleZones == 0) tackleZones = null;
	}

	public jsonTile() {
		
	}

	public int[] getPosition() {
		return position;
	}

	public void setPosition(int[] position) {
		this.position = position;
	}

	public Integer getTackleZones() {
		return tackleZones;
	}

	public void setTackleZones(Integer tackleZones) {
		  this.tackleZones = tackleZones;
	}

	public Integer getDodgeRoll() {
		return dodgeRoll;
	}

	public void setDodgeRoll(Integer dodgeRoll) {
		this.dodgeRoll = dodgeRoll;
	}

	public Integer getGoingForItRoll() {
		return goingForItRoll;
	}

	public void setGoingForItRoll(Integer goingForItRoll) {
		this.goingForItRoll = goingForItRoll;
	}

	public Integer getPickUpBallRoll() {
		return pickUpBallRoll;
	}

	public void setPickUpBallRoll(int pickUpBallRoll) {
		this.pickUpBallRoll = pickUpBallRoll;
	}

	public Integer getStandUpRoll() {
		return standUpRoll;
	}

	public void setStandUpRoll(int standUpRoll) {
		this.standUpRoll = standUpRoll;
	}
}
