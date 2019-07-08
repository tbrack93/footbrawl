package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import entity.Game;
import entity.Player;
import entity.Team;
import instance.PlayerInGame;
import instance.Tile;

// controls a game's logic and progress
// future: contain DTO for database interactions
public class GameService {

	// for finding neighbouring tiles
	private static final int[][] ADJACENT = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 },
			{ 1, 0 }, { 1, 1 } };
	private Game game;
	private int half;
	private String phase;
	private int activeTeam;
	private List<PlayerInGame> team1;
	private List<PlayerInGame> team2;
	private int team1Turn;
	private int team2Turn;
	private Tile[][] pitch;
	private boolean waitingForPlayers;

	public GameService(Game game) {
		this.game = game;
		setUpTeams();
		pitch = new Tile[26][15];
		for (int row = 0; row < 26; row++) {
			for (int column = 0; column < 15; column++) {
				pitch[row][column] = new Tile(row, column);
			}
		}
		setTileNeighbours(); // doing it once and saving in Tile objects saves repeated computations
	}

	public void setUpTeams() {
		team1 = new ArrayList<>();
		team2 = new ArrayList<>();
		for (Player p : game.getTeam1().getPlayers()) {
			team1.add((PlayerInGame) p);
		}
		for (Player p : game.getTeam2().getPlayers()) {
			team2.add((PlayerInGame) p);
		}
	}

	public List<Tile> getNeighbours(Tile t) {
		List<Tile> neighbours = new ArrayList<Tile>();
		int row = t.getPosition()[0];
		int column = t.getPosition()[1];
		for (int[] adjacentSquares : ADJACENT) {
			int tempR = row + adjacentSquares[1];
			int tempC = column + adjacentSquares[0];
			if (tempR >= 0 && tempR < 26 && tempC >= 0 && tempC < 15) {
				neighbours.add(pitch[tempR][tempC]);
			}
		}
		return neighbours;
	}

	public void setTileNeighbours() {
		for (Tile[] array : pitch) {
			for (Tile t : array) {
				t.setNeighbours(getNeighbours(t));
			}
		}
	}

	public void showPossibleMovement(PlayerInGame p) {
		resetTiles();
		Tile position = p.getTile();
		searchNeighbours(p, position, 0);
		for (int i = 0; i < 26; i++) {
			for (int j = 0; j < 15; j++) {
				Tile t = pitch[i][j];
				System.out.printf("%5d %2d ", t.getCostToReach(), t.getTackleZones());
			}
			System.out.println();
		}
	}

	// breadth first search to determine where can move
	public void searchNeighbours(PlayerInGame p, Tile location, int cost) {
		if (cost == p.getMA() + 2) {
			return;
		}

		for (Tile t : location.getNeighbours()) {
			if (!t.containsPlayer()) {
				int currentCost = t.getCostToReach();

				// checking if visited (not default of 99) or visited and new has route better
				// cost
				if (currentCost == 99 || currentCost != 99 && currentCost > cost + 1) {
					if (cost + 1 > p.getMA()) {
						t.goForIt();
					} else {
						t.setCostToReach(cost + 1);
					}
					searchNeighbours(p, t, cost + 1);
				}
			} else if (t.getPlayer() == p) {
				t.setCostToReach(0);
			} else if (t.getPlayer().getTeam() != p.getTeam() && t.getPlayer().hasTackleZones()) {
				addTackleZones(t.getPlayer(), p);
			}
		}
	}

	// An A star algorithm for Player to get from a to b, favouring avoiding tackle
	// zones and going for it
	public void getOptimisedPath(PlayerInGame p, int[] goal) throws IllegalArgumentException {
		Tile origin = p.getTile();
		// Tile origin = selectedPlayer.getTile();
		Tile target = pitch[goal[0]][goal[1]];
		int MA = origin.getPlayer().getMA();

		Comparator<Tile> comp = new Comparator<Tile>() {
			@Override
			public int compare(Tile t1, Tile t2) {
				return Double.compare((t1.getWeightedDistance() + t1.getHeuristicDistance()),
						(t2.getWeightedDistance() + t2.getHeuristicDistance()));
			}
		};
		Queue<Tile> priorityQueue = new PriorityQueue<>(comp);

		// enque StartNode, with distance 0
		for (Tile array[] : pitch) {
			for (Tile t : array) {
				t.setWeightedDistance(1000);
				t.setTotalDistance(1000.0);
				t.setParent(null);
			}
		}
		origin.setWeightedDistance(0.0);
		origin.setTotalDistance(0.0);
		priorityQueue.add(origin);
		Tile current = null;

		while (!priorityQueue.isEmpty()) {
			current = priorityQueue.remove();

			if (!current.isVisited()) {
				current.setVisited(true);
				// if last element in PQ reached
				if (current.equals(target)) {
					showTravelPath(p, origin, target);
					return;
				}

				List<Tile> neighbours = getNeighbours(current);
				for (Tile neighbour : neighbours) {
					if (!neighbour.isVisited()) {

						// calculate predicted distance to the end node
						double predictedDistance = Math.abs((neighbour.getPosition()[0] - target.getPosition()[0]))
								+ Math.abs((neighbour.getPosition()[1] - target.getPosition()[1]));

						// Penalties to prevent invalid moves and prefer not entering tackle zones or
						// Going For It
						int movementToReach = current.getMovementUsed() + 1;
						int neighbourCost = neighbour.getPlayer() != null ? 10000 : 1;
						int noMovementPenalty = movementToReach > MA + 2 ? 10000 : 0;
						int goForItPenalty = movementToReach > MA ? (movementToReach - MA) * 4 : 0;
						int tackleZonesPenalty = Math.abs(neighbour.getTackleZones()) * 4;

						double totalDistance = current.getWeightedDistance() + neighbourCost + goForItPenalty
								+ noMovementPenalty + tackleZonesPenalty + predictedDistance;
						// check if distance smaller
						if (totalDistance < neighbour.getTotalDistance()) {

							// update tile's distance
							neighbour.setTotalDistance(totalDistance);
							// used for PriorityQueue
							neighbour.setMovementUsed(movementToReach);
							neighbour.setWeightedDistance(totalDistance - predictedDistance);
							neighbour.setHeuristicDistance(predictedDistance);
							// set parent
							neighbour.setParent(current);
							// enqueue
							priorityQueue.add(neighbour);
						}
					}
				}
			}
		}
		throw new IllegalArgumentException("Selected player cannot reach that point");
	}

	public void showTravelPath(Player p, Tile origin, Tile goal) {
		List<Tile> route = new ArrayList<Tile>();
		Tile current = goal;
		route.add(goal);
		while (current != origin) {
			current = current.getParent();
			route.add(current);
		}
		Collections.reverse(route);
		for (Tile t : route) {
			System.out.print(t.getPosition()[0] + " " + t.getPosition()[1]);
			System.out.println(t.getMovementUsed() > p.getMA() ? " Going For It" : "");
			if (t.getParent() != null) {
				if (t.getParent().getTackleZones() != 0)
					System.out.println("Dodge");
			}
		}
	}

	// need to ensure calculating tackle zones when asking for route data
	public void addTackleZones(PlayerInGame opponent, PlayerInGame selected) {
		Tile opponentLocation = opponent.getTile();
		for (Tile t : opponentLocation.getNeighbours()) {
			if (!t.containsPlayer() || t.getPlayer() == selected) {
				t.addTackler(opponent);
			}
		}
	}

	public void resetTiles() {
		for (Tile[] array : pitch) {
			for (Tile t : array) {
				t.resetMovement();
			}
		}
	}

	public static void main(String[] args) {
		Player p = new PlayerInGame();
		p.setName("Billy");
		p.setMA(1);
		p.setTeam(1);
		Player p2 = new PlayerInGame();
		p2.setName("Bobby");
		p2.setMA(3);
		p2.setTeam(2);
		Team team1 = new Team("bobcats");
		Team team2 = new Team("murderers");
		team1.addPlayer(p);
		team2.addPlayer(p2);
		Game g = new Game();
		g.setTeam1(team1);
		g.setTeam2(team2);
		GameService gs = new GameService(g);
		p = (PlayerInGame) p;
		gs.pitch[5][4].addPlayer((PlayerInGame) p);
		gs.pitch[5][5].addPlayer((PlayerInGame) p2);
		gs.showPossibleMovement((PlayerInGame) p);
		int[] goal = { 5, 6 };
		gs.getOptimisedPath((PlayerInGame) p, goal);
	}
}
