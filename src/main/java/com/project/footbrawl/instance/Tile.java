package com.project.footbrawl.instance;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tile {

	private int[] location;
	private List<Tile> neighbours;
	private PlayerInGame player;
	private boolean containsBall;
	private boolean empty;
	private boolean moveTo;
	private boolean goForIt;
	private int costToReach; // for if possible to move there (with shortest path)
	Set<PlayerInGame> tacklers;

	// for A star path finding
	private double weightedDistance; // cost of moves + any penalties (g)
	private double heuristicDistance; // estimated distance to goal (h)
	private double totalDistance; // weighted + heuristic distances (f)
	private int movementUsed;
	private Tile parent;
	private boolean visited;

	public double getHeuristicDistance() {
		return heuristicDistance;
	}

	public void setHeuristicDistance(double heuristicDistance) {
		this.heuristicDistance = heuristicDistance;
	}

	public Tile(int row, int column) {
		location = new int[] { row, column };
		empty = true;
		containsBall = false;
		moveTo = false;
		costToReach = 99;
		tacklers = new HashSet<>();
		movementUsed = 0;
		goForIt = false;
	}

	public void addPlayer(PlayerInGame p) {
		this.player = p;
		empty = false;
		p.setTile(this);
	}

	public PlayerInGame getPlayer() {
		return player;
	}

	public void removePlayer() {
		if(player != null) {
			player.setTile(null);
		}
	    player = null;
		empty = containsBall;
	}
	
	public void setPlayer(PlayerInGame p) {
		this.player = p;
	}

	public void addBall() {
		containsBall = true;
		empty = false;
	}

	public void removeBall() {
		containsBall = false;
		empty = (player == null);
	}

	public boolean isEmpty() {
		return empty;
	}

	public boolean containsPlayer() {
		return (player != null);
	}

	public boolean getMoveTo() {
		return moveTo;
	}

	public int getCostToReach() {
		return costToReach;
	}

	public void setCostToReach(int cost) {
		costToReach = cost;
	}

	public void goForIt() {
		goForIt = true;
	}
	
	public void setGoForIt(boolean gfi) {
		goForIt = gfi;
	}

	public void moveTo() {
		moveTo = true;
	}

	public int[] getLocation() {
		return location;
	}

	public void resetMovement() {
		moveTo = false;
		goForIt = false;
		costToReach = 99;
		tacklers.clear();
		if(player != null && player.getTile() != this) {
			player.getTile().addPlayer(player);
			player = null;
		}
	}

	public void addTackler(PlayerInGame p) {
		tacklers.add(p);
	}
	
	public void removeTackler(PlayerInGame p) {
		tacklers.remove(p);
	}
	
	public void clearTacklers() {
		tacklers.clear();
	}
	
	public Set<PlayerInGame> getTacklers(){
		return tacklers;
	}

	public int getTackleZones() {
		return -tacklers.size();
	}

	public double getWeightedDistance() {
		return weightedDistance;
	}

	public void setWeightedDistance(double distance) {
		weightedDistance = distance;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(location);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tile other = (Tile) obj;
		if (!Arrays.equals(location, other.location))
			return false;
		return true;
	}

	public int getMovementUsed() {
		return movementUsed;
	}

	public void setMovementUsed(int movementUsed) {
		this.movementUsed = movementUsed;
	}

	public List<Tile> getNeighbours() {
		return neighbours;
	}

	public void setNeighbours(List<Tile> neighbours) {
		this.neighbours = neighbours;
	}

	public Tile getParent() {
		return parent;
	}

	public void setParent(Tile parent) {
		this.parent = parent;
	}

	public double getTotalDistance() {
		return totalDistance;
	}

	public void setTotalDistance(double totalDistance) {
		this.totalDistance = totalDistance;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public boolean getGoForIt() {
		return goForIt;
	}
	
	public void setContainsBall(boolean ball) {
		this.containsBall = ball;
	}
	
	public boolean containsBall() {
		return containsBall;
	}
}
