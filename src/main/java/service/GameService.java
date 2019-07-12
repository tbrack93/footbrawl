package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import javax.print.attribute.standard.Destination;

import entity.Game;
import entity.Player;
import entity.Team;
import instance.PlayerInGame;
import instance.TeamInGame;
import instance.Tile;

// controls a game's logic and progress
// future: contain DTO for database interactions
public class GameService {

	// for finding neighbouring tiles
	private static final int[][] ADJACENT = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 },
			{ 1, 0 }, { 1, 1 } };
	private static final int[][] TOPLEFTTHROW = { { 0, 1 }, { 1, 1 }, { 1, 0 } };
	private static final int[][] TOPRIGHTTHROW = { { 0, -1 }, { 1, -1 }, { 1, 0 } };
	private static final int[][] BOTTOMLEFTTHROW = { { -1, 0 }, { -1, 1 }, { 0, 1 } };
	private static final int[][] BOTTOMRIGHTTHROW = { { -1, 0 }, { -1, -1 }, { 0, -1 } };
	private Game game;
	private int half;
	private String phase;
	private int activeTeam;
	private TeamInGame team1;
	private TeamInGame team2;
	private List<PlayerInGame> dugout;
	private int team1Turn;
	private int team2Turn;
	private Tile[][] pitch;
	private boolean waitingForPlayers;
	private Queue<Runnable> queue;

	public GameService(Game game) {
		queue = new LinkedList<>();
		dugout = new ArrayList<>();
		this.game = game;
		team1 = new TeamInGame(game.getTeam1());
		team2 = new TeamInGame(game.getTeam2());
		pitch = new Tile[26][15];
		for (int row = 0; row < 26; row++) {
			for (int column = 0; column < 15; column++) {
				pitch[row][column] = new Tile(row, column);
			}
		}
		setTileNeighbours(); // doing it once and saving in Tile objects saves repeated computations
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
		if (cost == p.getRemainingMA() + 2) {
			return;
		}
		addTackleZones(p);
		for (Tile t : location.getNeighbours()) {
			if (!t.containsPlayer()) {
				int currentCost = t.getCostToReach();

				// checking if visited (not default of 99) or visited and new has route better
				// cost
				if (currentCost == 99 || currentCost != 99 && currentCost > cost + 1) {
					if (cost + 1 > p.getRemainingMA()) {
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
					return getTravelPath(p, origin, target);
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
//						System.out.println("Current " + current.getPosition()[0] + " " +current.getPosition()[1]);
//						System.out.println("Neighbour " + neighbour.getPosition()[0] + " " + neighbour.getPosition()[1]);
//						System.out.println("Total:" + totalDistance);
//						System.out.println("Current:" + neighbour.getTotalDistance());
//						System.out.println("No Movement penalty? " + noMovementPenalty);
//						System.out.println(" Tacklezones penalty?" + tackleZonesPenalty);
//						System.out.println("Predicted distance " + predictedDistance);
//						System.out.println();
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

	public List<Tile> getTravelPath(PlayerInGame p, Tile origin, Tile goal) {
		List<Tile> route = new ArrayList<Tile>();
		Tile current = goal;
		route.add(goal);
		while (current != origin) {
			current = current.getParent();
			route.add(current);
		}
		Collections.reverse(route);
		return route;
	}

	public void showTravelPath(List<Tile> route) {
		PlayerInGame p = route.get(0).getPlayer();
		addTackleZones(p);
		for (int i = 0; i < route.size(); i++) {
			Tile t = route.get(i);
			System.out.print("\n" + t.getPosition()[0] + " " + t.getPosition()[1]);
			System.out.print(i > p.getRemainingMA() ? " Going For It: 2+" : "");
			if (i > 0) {
				if (route.get(i - 1).getTackleZones() != 0)
					System.out.print(" Dodge: " + calculateDodge(p, route.get(i - 1)) + "+");
			}
			if (t.containsBall()) {
				System.out.print(" Pick Up Ball: " + calculatePickUpBall(p, t) + "+");
			}
		}
	}

	public List<Tile> getRouteWithWaypoints(PlayerInGame p, int[][] waypoints, int[] goal) {
		int startingMA = p.getRemainingMA();
		List<Tile> totalRoute = new ArrayList<>();
		Tile origin = p.getTile();
		List<Tile> forReset = new ArrayList<>();
		for (int[] i : waypoints) {
			totalRoute.addAll(getOptimisedPath(p, i));
			totalRoute.remove(totalRoute.size() - 1); // removes duplicate tiles
			p.setRemainingMA(p.getRemainingMA() - totalRoute.size());
			Tile t = pitch[i[0]][i[1]];
			if (!t.containsPlayer()) {
				t.addPlayer(p);
				forReset.add(t);
			}

		}
		totalRoute.addAll(getOptimisedPath(p, goal));
		p.setRemainingMA(startingMA);
		origin.addPlayer(p);
		for (Tile t : forReset) {
			t.removePlayer();
		}
		// queue.add(() -> getRouteWithWaypoints((PlayerInGame) p, waypoints, goal));
		return totalRoute;
	}

	public void movePlayerRoute(PlayerInGame p, List<Tile> route) {
		addTackleZones(p);
		checkRouteValid(p, route);
		route.remove(0);
		for (Tile t : route) {
			Tile tempT = p.getTile();
			t.addPlayer(p);
			tempT.removePlayer();
			p.decrementRemainingMA();
			if (p.getRemainingMA() < 0) {
				if (!goingForItAction(p, tempT, t)) {
					rerollCheck();
					return;
				}
			}
			if (tempT.getTackleZones() != 0) {
				if (!dodgeAction(p, tempT, t)) {
					rerollCheck();
					return;
				}
			}
			if (tempT.containsBall()) {
				pickUpBallAction(p);
			} else {
				System.out.println(p.getName() + " moved to: " + t.getPosition()[0] + " " + t.getPosition()[1]);
			}
		}
	}

	private void checkRouteValid(PlayerInGame p, List<Tile> route) {
		List<Tile> tempR = new ArrayList<>(route);
		int startingMA = p.getRemainingMA();
		if (p.getTile() != tempR.get(0)) {
			throw new IllegalArgumentException("Route does not start from player's current position");
		}
		tempR.remove(0);
		for (Tile t : tempR) {
			if (t.containsPlayer()) {
				p.setRemainingMA(startingMA);
				throw new IllegalArgumentException("Can't move to occupied square");
			}
			p.decrementRemainingMA();
			if (p.getRemainingMA() < -2) {
				throw new IllegalArgumentException("Not enough movement to reach destination");
			}
		}
	}

	private boolean goingForItAction(PlayerInGame p, Tile tempT, Tile t) {
		int result = diceRoller(1, 6)[0];
		if (result >= 2) {
			System.out.println(p.getName() + " went for it!");
			return true;
		} else {
			System.out.println(p.getName() + " went for it and tripped!");
			knockDown(p);
			return false;
		}
	}

	public boolean dodgeAction(PlayerInGame p, Tile from, Tile to) {
		int roll = calculateDodge(p, from);
		int result = diceRoller(1, 6)[0];
		System.out.println("Needed " + roll + "+" + " Rolled: " + result);
		if (result >= roll) {
			System.out.println(p.getName() + " dodged from " + from.getPosition()[0] + " " + from.getPosition()[1]
					+ " to " + to.getPosition()[0] + " " + to.getPosition()[1] + " with a roll of " + result);
			return true;
		} else {
			System.out.println(p.getName() + " failed to dodge and was tripped into " + to.getPosition()[0] + " "
					+ to.getPosition()[1]);
			knockDown(p);
			return false;
		}
	}

	public int calculateDodge(PlayerInGame p, Tile from) {
		addTackleZones(p);
		int AG = p.getAG();
		int modifier = from.getTackleZones();
		int result = 7 - AG - 1 - modifier;
		if (result <= 1)
			result = 2; // roll of 1 always fails, no matter what
		if (result > 6)
			result = 6; // roll of 6 always passes, no matter what
		return result;
	}

	public void knockDown(PlayerInGame p) {
		p.setProne();
		if (p.hasBall()) {
			scatterBall(p.getTile(), 1);
			p.setHasBall(false);
		}
		int armour = p.getAV();
		int[] rolls = diceRoller(2, 6);
		int total = rolls[0] + rolls[1];
		if (total > armour) {
			System.out.println(p.getName() + "'s armour was broken");
			rolls = diceRoller(2, 6);
			total = rolls[0] + rolls[1];
			if (total <= 7) {
				System.out.println(p.getName() + " is stunned");
				p.setStatus("stunned");
			} else {
				// possibility to use apothecary, etc. here
				p.getTile().removePlayer();
				dugout.add(p);
				if (total <= 9) {
					System.out.println(p.getName() + " is KO'd");
					p.setStatus("KO");
				} else {
					System.out.println(p.getName() + " is injured");
					p.setStatus("injured");
				}
			}
		} else {
			System.out.println("Armour held");
			return;
		}
	}

	public void rerollCheck() {
		// placeholder
	}

	// if times > 1 cannot try to catch until final scatter
	public void scatterBall(Tile origin, int times) {
		int value = diceRoller(1, 8)[0];
		int[] direction = ADJACENT[value - 1];
		int[] position = new int[] { origin.getPosition()[0] + direction[0], origin.getPosition()[1] + direction[1] };
		if (position[0] > 0 && position[0] < 26 && position[1] >= 0 && position[1] < 15) {
			Tile target = pitch[position[0]][position[1]];
			System.out.println("Ball scattered to: " + position[0] + " " + position[1]);
			if (times > 1) {
				scatterBall(target, times - 1);
				return;
			}
			if (target.containsPlayer()) {
				if (target.getPlayer().hasTackleZones()) { // will need to make this more specific to catching
					catchBallAction(target.getPlayer(), false);
				} else {
					scatterBall(target, 1); // if player can't catch, will scatter again
				}
			} else {
				origin.setContainsBall(false);
				target.setContainsBall(true); // will need a message to inform front end of this ball movement
			}
		} else {
			ballOffPitch(origin);
		}
	}

	public void catchBallAction(PlayerInGame player, boolean accuratePass) {
		int needed = calculateCatch(player, accuratePass);
		int roll = diceRoller(1, 6)[0];
		System.out.println(player.getName() + " tries to catch the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " caught the ball!");
			player.setHasBall(true);
		} else {
			System.out.println(player.getName() + " failed to catch the ball!");
			rerollCheck();
			scatterBall(player.getTile(), 1);
		}
	}
	
	public boolean interceptBallAction(PlayerInGame player) {
		int needed = calculateInterception(player);
		int roll = diceRoller(1,6)[0];
		System.out.println(player.getName() + " tries to intercept the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " intercepted the ball!");
			player.setHasBall(true);
			return true;
		} else {
			System.out.println(player.getName() + " failed to intercept the ball!");
			rerollCheck();
			return false;
		}
	}

	public void pickUpBallAction(PlayerInGame player) {
		if (!player.getTile().containsBall()) {
			throw new IllegalArgumentException("Player not in square with the ball");
		}
		int needed = calculatePickUpBall(player, player.getTile());
		int roll = diceRoller(1, 6)[0];
		System.out.println(player.getName() + " tries to pick up the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " picked up the ball!");
			player.setHasBall(true);
		} else {
			System.out.println(player.getName() + " failed to pick up the ball!");
			rerollCheck();
			scatterBall(player.getTile(), 1);
		}
	}

	public void throwBallAction(PlayerInGame player, Tile target) {
		if (!player.hasBall()) {
			throw new IllegalArgumentException("Player doesn't have the ball");
		}
		List<Tile> path = calculateThrowTiles(player, player.getTile(), target);
		List<PlayerInGame> interceptors = calculatePossibleInterceptors(path, player);
		PlayerInGame interceptor = null;
		if (interceptors.size() > 1) {
			requestInterceptor();
		} else if (interceptors.size() == 1) {
			interceptor = interceptors.get(0);
		}
		int needed = calculateThrow(player, player.getTile(), target);
		int roll = diceRoller(1, 6)[0];
		System.out.println(player.getName() + " tries to throw the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll == 1) {
			System.out.println(player.getName() + " fumbled the ball!");
			rerollCheck();
			scatterBall(player.getTile(), 1);
		} else {
			if(interceptor != null) {
				if(interceptBallAction(interceptor)) {
				  return;
				}
			}
			if (roll >= needed) {
				System.out.println(player.getName() + " threw the ball accurately!");
				player.setHasBall(false);
				if (target.containsPlayer()) {
					catchBallAction(target.getPlayer(), true);
				} else {
					target.setContainsBall(true);
				}
			} else {
				System.out.println(player.getName() + " threw the ball badly");
				rerollCheck();
				scatterBall(target, 3);
			}
		}
	}

	public int calculateCatch(PlayerInGame p, boolean accuratePass) {
		int extraModifier = accuratePass ? 1 : 0;
		return calculateAgilityRoll(p, p.getTile(), extraModifier);
	}

	public int calculateInterception(PlayerInGame p) {
		return calculateAgilityRoll(p, p.getTile(), -2);
	}

	public int calculatePickUpBall(PlayerInGame p, Tile location) {
		return calculateAgilityRoll(p, location, 1);
	}

	public int calculateThrow(PlayerInGame thrower, Tile from, Tile target) {
		int[] origin = from.getPosition();
		int[] destination = target.getPosition();
		// rounds distance to target to nearest square (in a straight line, using
		// Pythagoras' theorem)
		int distance = (int) Math.sqrt(((origin[0] - destination[0]) * (origin[0] - destination[0]))
				+ ((origin[0] - destination[0]) * (origin[0] - destination[0])));
		int distanceModifier = 0;// short pass
		if (distance > 13) {
			throw new IllegalArgumentException("Cannot throw more than 13 squares");
		} else if (distance < 4) { // quick pass
			distanceModifier = 1;
		} else if (distance > 6 && distance < 11) { // long pass
			distanceModifier = -1;
		} else if (distance > 11) { // bomb
			distanceModifier = -2;
		}
		List<Tile> path = calculateThrowTiles(thrower, from, target);
		calculatePossibleInterceptors(path, thrower);
		return calculateAgilityRoll(thrower, from, distanceModifier);
	}

	public List<PlayerInGame> calculatePossibleInterceptors(List<Tile> path, PlayerInGame thrower) {
		List<PlayerInGame> interceptors = new ArrayList<>();
		for (Tile t : path) {
			if (t.containsPlayer() && t.getPlayer().getTeam() != thrower.getTeam() && t.getPlayer().hasTackleZones()) {
				interceptors.add(t.getPlayer());
				System.out.println("Possible interception by " + t.getPlayer().getName() + " at " + t.getPosition()[0]
						+ " " + t.getPosition()[1] + " with a roll of " + calculateInterception(t.getPlayer()) + "+");
			}
		}
		return interceptors;
	}

	public void requestInterceptor() {
		// placeholder
	}

	// getting all squares that ball will travel over, for interception options
	// uses enhanced version of Bresenham's line algorithm. Adapted from
	// http://playtechs.blogspot.com/2007/03/raytracing-on-grid.html
	public List<Tile> calculateThrowTiles(PlayerInGame thrower, Tile from, Tile target) {
		List<Tile> squares = new ArrayList<>();
		int x = from.getPosition()[0];
		int y = from.getPosition()[1];
		int xDistance = Math.abs(x - target.getPosition()[0]);
		int yDistance = Math.abs(y - target.getPosition()[1]);
		int n = 1 + xDistance + yDistance;
		int xIncline = (target.getPosition()[0] > x) ? 1 : -1;
		int yIncline = (target.getPosition()[1] > y) ? 1 : -1;
		int error = xDistance - yDistance;
		xDistance *= 2;
		yDistance *= 2;

		for (; n > 0; --n) {
			squares.add(pitch[x][y]);
			if (error > 0) {
				x += xIncline;
				error -= yDistance;
			} else {
				y += yIncline;
				error += xDistance;
			}
		}
		return squares;
	}

	public int calculateAgilityRoll(PlayerInGame p, Tile location, int extraModifier) {
		addTackleZones(p);
		int AG = p.getAG();
		int modifier = location.getTackleZones();
		int result = 7 - AG - extraModifier - modifier;
		if (result <= 1)
			result = 2; // roll of 1 always fails, no matter what
		if (result > 6)
			result = 6; // roll of 6 always passes, no matter what
		return result;
	}

	// calculates throw direction, to save from having more constants
	// no apparent way to calculate for corners, so use constant arrays for these
	public void ballOffPitch(Tile origin) {
		System.out.println("Ball went off pitch from " + origin.getPosition()[0] + " " + origin.getPosition()[1]);
		// determine which side/ orientation
		int[] position = origin.getPosition();
		int[] direction = new int[2];
		int[][] corner = null;
		int shift = 1;
		if (position[0] == 0) { // from top
			if (position[1] == 0) {
				corner = TOPLEFTTHROW;
			} else if (position[1] == 14) {
				corner = TOPRIGHTTHROW;
			} else {
				direction[0] = 1;
			}
		}
		if (position[0] == 25) { // from bottom
			if (position[1] == 0) {
				corner = BOTTOMLEFTTHROW;
			} else if (position[1] == 14) {
				corner = BOTTOMRIGHTTHROW;
			} else {
				direction[0] = -1;
			}
		}
		if (position[1] == 0) { // from left
			direction[1] = 1;
			shift = 0;
		}
		if (position[1] == 14) { // from right
			direction[1] = -1;
			shift = 0;
		}
		// roll 1D3 to determine direction
		int directionRoll = diceRoller(1, 3)[0];
		if (corner != null) {
			direction = corner[directionRoll - 1];
		} else {
			direction[shift] = directionRoll - 2;
		}
		System.out.println("Rolled direction: " + directionRoll);
		System.out.println("Direction: " + direction[0] + " " + direction[1]);

		// roll 2D6 to determine squares moved
		int[] squares = diceRoller(2, 6);
		int squaresTotal = squares[0] + squares[1];
		System.out.println("Rolled to move " + squaresTotal + " squares");
		int[] destination = Arrays.copyOf(position, 2);

		for (int i = 0; i < squaresTotal; i++) {
			destination[0] = destination[0] + direction[0];
			destination[1] = destination[1] + direction[1];
			System.out.println("Ball flying to " + destination[0] + " " + destination[1]);
			if (destination[0] < 0 || destination[0] > 25 || destination[1] < 0 || destination[1] > 14) {
				System.out.println("Ball thrown off pitch again!");
				System.out.println("Destination: " + destination[0] + " " + destination[1]);
				destination[0] -= direction[0];
				destination[1] -= direction[1];
				ballOffPitch(pitch[destination[0]][destination[1]]);
				return;
			}
		}
		Tile target = pitch[destination[0]][destination[1]];
		System.out.println("Ball thrown to square " + destination[0] + " " + destination[1]);
		if (target.containsPlayer()) {
			if (target.getPlayer().hasTackleZones()) {
				catchBallAction(target.getPlayer(), false);
			} else {
				scatterBall(target, 1);
			}
		} else {
			target.setContainsBall(true);
		}
	}

	public void addTackleZones(PlayerInGame activePlayer) {
		resetTackleZones();
		List<PlayerInGame> opponents;
		opponents = activePlayer.getTeam() == game.getTeam1().getId() ? team2.getPlayersOnPitch()
				: team1.getPlayersOnPitch();
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

	// result: first element is dice to roll, second element id of team (user) to
	// choose result
	public int[] calculateBlock(PlayerInGame attacker, PlayerInGame defender) {
		if (!attacker.getTile().getNeighbours().contains(defender.getTile())) {
			throw new IllegalArgumentException("Can only block an adjacent player");
		}
		;
		if (attacker.getTeam() == defender.getTeam()) {
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
		List<PlayerInGame> support = new ArrayList<>(p2.getTile().getTacklers());
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
		Player p = new Player();
		p.setName("Billy");
		p.setMA(4);
		p.setAG(2);
		p.setTeam(1);
		p.setST(6);
		Player p2 = new Player();
		p2.setName("Bobby");
		p2.setAG(10);
		p2.setMA(3);
		p2.setTeam(2);
		p2.setST(3);
		Player p3 = new Player();
		p3.setName("Sam");
		p3.setMA(3);
		p3.setTeam(2);
		p3.setST(3);
		Player p4 = new Player();
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
		List<PlayerInGame> team1Players = gs.team1.getPlayersOnPitch();
		List<PlayerInGame> team2Players = gs.team2.getPlayersOnPitch();
		gs.pitch[7][7].addPlayer(team1Players.get(0));
		gs.pitch[17][5].addPlayer(team1Players.get(1));
		gs.pitch[5][5].addPlayer(team2Players.get(0));
		gs.pitch[5][3].addPlayer(team2Players.get(1));
		Tile ballTile = gs.pitch[7][7];
		ballTile.setContainsBall(true);
		gs.pickUpBallAction(team1Players.get(0));
		gs.throwBallAction(team1Players.get(0), gs.pitch[3][3]);

		 List<Tile> squares = gs.calculateThrowTiles(team1Players.get(0),
		 team1Players.get(0).getTile(), gs.pitch[3][3]);
//		for(Tile t : squares) {
//			System.out.println(t.getPosition()[0] + " " + t.getPosition()[1]);
//		}
		//List<PlayerInGame> interceptors = gs.calculatePossibleInterceptors(squares, team1Players.get(0));
		//System.out.println(interceptors.size());
		
		// gs.showPossibleMovement(team1Players.get(0))
		// int[] goal = { 9, 9 };
		// List<Tile> route = gs.getOptimisedPath(team1Players.get(0), goal);
		// gs.showTravelPath(route);
		// gs.getOptimisedPath((PlayerInGame) p, goal);
		// int[][] waypoints = { { 5, 6 }, { 7, 7 } };
		// List<Tile> route = gs.getRouteWithWaypoints(team1Players.get(0), waypoints,
		// goal);
		// gs.showTravelPath(route);
//		for(Tile t : route) {
//			System.out.println(" Main: " + t.getPosition()[0] + " " + t.getPosition()[1]);
//		}
		// gs.movePlayerRoute(team1Players.get(0), route);
		// gs.getRouteWithWaypoints((PlayerInGame) p, waypoints, goal);
		// gs.queue.remove().run();
		// System.out.println(gs.calculateDodge((PlayerInGame)p, gs.pitch[6][5]) +"+");
		// int[] results = diceRoller(2, 3);
		System.out.println();
//		for(int i = 0; i<results.length; i++) {
//			System.out.print(results[i] + " ");
//		}
//		int[] block = gs.calculateBlock((PlayerInGame)p, (PlayerInGame) p2);
//		for(int i = 0; i<results.length; i++) {
//			System.out.print(block[i] + " ");
//		}
		// Tile t = gs.pitch[25][14];
		// gs.ballOffPitch(t);

	}
}
