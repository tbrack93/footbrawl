package instance;

import entity.Player;

public class PlayerInGame extends Player {
	
	private int remainingMA;
	private boolean actedThisTurn;
	private boolean actionOver;
	private String status;
	private boolean hasTackleZones;
	private boolean hasBall;
	private Tile tile;
	
	
	public PlayerInGame() {
		super();
		hasTackleZones = true;
	}
	
	public void newTurn() {
		remainingMA = this.getMA();
		actedThisTurn = false;
		actionOver = false;
	}
	
	@Override
	public void setMA(int MA) {
		super.setMA(MA);;
		this.remainingMA = MA;
	}

	public int getRemainingMA() {
		return remainingMA;
	}

	public void setRemainingMA(int remainingMA) {
		this.remainingMA = remainingMA;
	}
	
	public void decrementRemainingMA() {
		this.remainingMA--;
	}

	public boolean isActedThisTurn() {
		return actedThisTurn;
	}

	public void setActedThisTurn(boolean actedThisTurn) {
		this.actedThisTurn = actedThisTurn;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public void setProne() {
		status = "prone";
		hasTackleZones = false;
		actedThisTurn = true;
		actionOver = true;
	}

	public boolean hasTackleZones() {
		return hasTackleZones;
	}

	public void setHasTackleZones(boolean hasTackleZones) {
		this.hasTackleZones = hasTackleZones;
	}

	public Tile getTile() {
		return tile;
	}

	public void setTile(Tile tile) {
		this.tile = tile;
	}
	
	public void setHasBall(boolean ball) {
		this.hasBall = ball;
	}
	
	public boolean hasBall() {
		return hasBall;
	}
	
	
	
}
