package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

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
    private Queue<Runnable> queue;

	public GameService(Game game) {
		queue = new LinkedList<>();
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
				int tackleZones = t.getTackleZones();
				if (t.getCostToReach() == 99)
					tackleZones = 0;
				System.out.printf("%5d %2d ", t.getCostToReach(), tackleZones);
			}
			System.out.println();
		}
	}

	// breadth first search to determine where can move
	public void searchNeighbours(PlayerInGame p, Tile location, int cost) {
		if (cost == p.getMA() + 2) {
			return;
		}
		addTackleZones(p);
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
			}
		}
	}

	// An A star algorithm for Player to get from a to b, favouring avoiding tackle
	// zones and going for it
	public List<Tile> getOptimisedPath(PlayerInGame p, int[] goal) throws IllegalArgumentException {
		addTackleZones(p);
		Tile origin = p.getTile();
		// Tile origin = selectedPlayer.getTile();
		Tile target = pitch[goal[0]][goal[1]];
		int MA = p.getRemainingMA();

		Comparator<Tile> comp = new Comparator<Tile>() {
			@Override
			public int compare(Tile t1, Tile t2) {
				return Double.compare((t1.getWeightedDistance() + t1.getHeuristicDistance()),
						(t2.getWeightedDistance() + t2.getHeuristicDistance()));
			}
		};
		Queue<Tile> priorityQueue = new PriorityQueue<>(comp);

		for (Tile array[] : pitch) {
			for (Tile t : array) {
				t.setWeightedDistance(1000);
				t.setTotalDistance(1000.0);
				t.setHeuristicDistance(0.0);
				t.setMovementUsed(0);
				t.setParent(null);
				t.setVisited(false);
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
					return showTravelPath(p, origin, target);
				}

				List<Tile> neighbours = getNeighbours(current);
				for (Tile neighbour : neighbours) {
					if (!neighbour.isVisited()) {

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

	public List<Tile> showTravelPath(PlayerInGame p, Tile origin, Tile goal) {
		List<Tile> route = new ArrayList<Tile>();
		Tile current = goal;
		route.add(goal);
		while (current != origin) {
			current = current.getParent();
			route.add(current);
		}
		Collections.reverse(route);
		for (Tile t : route) {
			System.out.print("\n" + t.getPosition()[0] + " " + t.getPosition()[1]);
			System.out.print(t.getMovementUsed() > p.getMA() ? " Going For It: 2+" : "");
			if (t.getParent() != null) {
				if (t.getParent().getTackleZones() != 0)
					System.out.print(" Dodge: " + calculateDodge(p, t.getParent()) + "+");
			}
		}
		return route;
	}
	
	public List<Tile> getRouteWithWaypoints(PlayerInGame p, int[][] waypoints, int[] goal) {
		List<Tile> totalRoute = new ArrayList<>();
		Tile origin = p.getTile();
		List<Tile> forReset = new ArrayList<>();
		for(int[] i : waypoints) {
			totalRoute.addAll(getOptimisedPath(p, i));
			totalRoute.remove(totalRoute.size()-1);
			p.setRemainingMA(p.getMA()-totalRoute.size());
			Tile t = pitch[i[0]][i[1]];
			if(!t.containsPlayer()) {
				t.addPlayer(p);
				forReset.add(t);
			}
			
		}
		totalRoute.addAll(getOptimisedPath(p, goal));
		p.setRemainingMA(p.getMA());
		origin.addPlayer(p);
		for(Tile t : forReset) {
			t.removePlayer();
		}
		queue.add(() -> getRouteWithWaypoints((PlayerInGame) p, waypoints, goal));
		return totalRoute;
	}
	
	public void movePlayerRoute(PlayerInGame p, List<Tile> route) {
		addTackleZones(p);
		if(p.getTile() != route.get(0)) {
			throw new IllegalArgumentException("Route does not start from player's current position");
		}
		route.remove(0);
		for(Tile t : route) {
			if(t.containsPlayer()) {
				throw new IllegalArgumentException("Can't move to occupied square");
			}
			Tile tempT = p.getTile();
			t.addPlayer(p);
			tempT.removePlayer();
			p.decrementMA();
			if(tempT.getTackleZones()!= 0) {
				if(!dodgeAction(p, tempT, t)){
			        rerollCheck();
					return;
				}
			} else {
			System.out.println(p.getName() + " moved to: " + t.getPosition()[0] + " " + 
					          t.getPosition()[1]);
			}
		}
	}

	public boolean dodgeAction(PlayerInGame p, Tile from, Tile to) {
		int roll = calculateDodge(p, from);
		int result = diceRoller(1, 6)[0];
		System.out.println("Needed " + roll + "+" + " Rolled: " + result);
		if(result>=roll) {
			System.out.println(p.getName() + " dodged from " +
		                       from.getPosition()[0] + " " + 
		                       from.getPosition()[1] + " to " +
		                       to.getPosition()[0] + " " + 
		                       to.getPosition()[1] + " with a roll of " + roll);
			return true;
		}
		else {
			System.out.println(p.getName() + "failed to dodge and was tripped into " +
					                       to.getPosition()[0] + " " + 
                                           to.getPosition()[1]); 
			knockDown(p);
			return false;
		}
	}
	
	public void knockDown(PlayerInGame p) {
		p.setProne();
		// placeholder
	}
	
	public void rerollCheck() {
		// placeholder
	}

	public void addTackleZones(PlayerInGame activePlayer) {
		resetTackleZones();
		List<PlayerInGame> opponents;
		opponents = activePlayer.getTeam() == game.getTeam1().getId() ? team2 : team1;
		for (PlayerInGame p : opponents) {
			if (p.hasTackleZones()) {
			  for (Tile t : p.getTile().getNeighbours()) {
			    t.addTackler(p);
			  }
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
	
	public void resetTackleZones() {
		for (Tile[] array : pitch) {
			for (Tile t : array) {
				t.clearTacklers();
			}
		}
	}

	public int calculateDodge(PlayerInGame p, Tile from) {
		int AG = p.getAG();
		int modifier = from.getTackleZones();
		int result = 7 - AG - 1 - modifier;
		if (result < 1)
			result = 1;
		if (result > 6)
			result = 6;
		return result;
	}

	// result: first element is dice to roll, second element id of team (user) to
	// choose result
	public int[] calculateBlock(PlayerInGame attacker, PlayerInGame defender) {
		if(!attacker.getTile().getNeighbours().contains(defender.getTile())){
			throw new IllegalArgumentException("Can only block an adjacent player");
		};
		if(attacker.getTeam() == defender.getTeam()){
			throw new IllegalArgumentException("Cannot block player on same team");
		}
		int[] assists = calculateAssists(attacker, defender);
		int attStr = attacker.getST() + assists[0];
		int defStr = defender.getST() + assists[1];
		int strongerTeam = attStr >= defStr ? attacker.getTeam() : defender.getTeam();
		int dice = 1;
		if (attStr >= defStr * 2 || defStr >= attStr * 2)
			dice = 3;
		if (attStr > defStr || defStr > attStr)
			dice = 2;
		return new int[] { dice, strongerTeam };
	}
	

	public int[] calculateAssists(PlayerInGame attacker, PlayerInGame defender) {
		attacker.setHasTackleZones(false);
		defender.setHasTackleZones(false);
		List<PlayerInGame> attSupport = getAssists(attacker, defender);
		List<PlayerInGame> defSupport = getAssists(defender, attacker);
		attacker.setHasTackleZones(true);
		defender.setHasTackleZones(true);
		return new int[] { attSupport.size(), defSupport.size() };
	}

	public List<PlayerInGame> getAssists(PlayerInGame p1, PlayerInGame p2) {
		addTackleZones(p2);
		List<PlayerInGame>support = new ArrayList<>(p2.getTile().getTacklers());
		for (PlayerInGame p : support) {
			addTackleZones(p);
			Set<PlayerInGame> tacklers = p.getTile().getTacklers();
			for (PlayerInGame q : tacklers) {
				addTackleZones(q);
				if (q.getTile().getTacklers().isEmpty()) {
					support.remove(p);
				}
			}
		}
		return support;
	}

	public static int[] diceRoller(int quantity, int number) {
		Random rand = new Random();
		int[] result = new int[quantity];
		for (int i = 0; i < quantity; i++) {
			result[i] = rand.nextInt(number) + 1;
		}
		return result;
	}

	public static void main(String[] args) {
		Player p = new PlayerInGame();
		p.setName("Billy");
		p.setMA(10);
		p.setAG(5);
		p.setTeam(1);
		p.setST(6);
		Player p2 = new PlayerInGame();
		p2.setName("Bobby");
		p2.setMA(3);
		p2.setTeam(2);
		p2.setST(3);
		Player p3 = new PlayerInGame();
		p3.setName("Sam");
		p3.setMA(3);
		p3.setTeam(2);
		p3.setST(3);
		Player p4 = new PlayerInGame();
		p3.setName("Sarah");
		p3.setMA(3);
		p3.setTeam(1);
		p3.setST(3);
		Team team1 = new Team("bobcats");
		Team team2 = new Team("murderers");
		team1.addPlayer(p);
		team2.addPlayer(p2);
		team2.addPlayer(p3);
		team1.addPlayer(p4);
		Game g = new Game();
		g.setTeam1(team1);
		g.setTeam2(team2);
		GameService gs = new GameService(g);
		p = (PlayerInGame) p;
		gs.pitch[5][4].addPlayer((PlayerInGame) p);
		gs.pitch[5][5].addPlayer((PlayerInGame) p2);
		gs.pitch[5][3].addPlayer((PlayerInGame) p3);
		gs.pitch[17][5].addPlayer((PlayerInGame) p4);
		gs.showPossibleMovement((PlayerInGame) p);
		int[] goal = {9,9};
	    //gs.getOptimisedPath((PlayerInGame) p, goal);
	    //gs.getOptimisedPath((PlayerInGame) p, goal);
		int[][] waypoints = {{5,6}, {7,7}};
		List<Tile> route = gs.getRouteWithWaypoints((PlayerInGame) p, waypoints, goal);
//		for(Tile t : route) {
//			System.out.println(" Main: " + t.getPosition()[0] + " " + t.getPosition()[1]);
//		}
		gs.movePlayerRoute((PlayerInGame) p, route);
	    //gs.getRouteWithWaypoints((PlayerInGame) p, waypoints, goal);
	   // gs.queue.remove().run();
		// System.out.println(gs.calculateDodge((PlayerInGame)p, gs.pitch[6][5]) +"+");
		int[] results = diceRoller(2, 3);
		System.out.println();
//		for(int i = 0; i<results.length; i++) {
//			System.out.print(results[i] + " ");
//		}
//		int[] block = gs.calculateBlock((PlayerInGame)p, (PlayerInGame) p2);
//		for(int i = 0; i<results.length; i++) {
//			System.out.print(block[i] + " ");
//		}
		
		
	}
}
