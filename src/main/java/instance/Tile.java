package instance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tile {

	private int[] position;
	private List<Tile> neighbours;
	private PlayerInGame player;
	private boolean containsBall;
	private boolean empty;
	private boolean moveTo;
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
		position = new int[] { row, column };
		empty = true;
		containsBall = false;
		moveTo = false;
		costToReach = 99;
		tacklers = new HashSet<>();
		movementUsed = 0;
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
		player.setTile(null);
		this.player = null;
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
		costToReach = 77;
	}

	public void moveTo() {
		moveTo = true;
	}

	public int[] getPosition() {
		return position;
	}

	public void resetMovement() {
		moveTo = false;
		costToReach = 99;
		tacklers.clear();
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
		result = prime * result + Arrays.hashCode(position);
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
		if (!Arrays.equals(position, other.position))
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
		return costToReach == 77;
	}
	
	public void setContainsBall(boolean ball) {
		this.containsBall = ball;
	}
	
	public boolean containsBall() {
		return containsBall;
	}

}
