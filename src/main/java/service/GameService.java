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

import entity.Game;
import entity.Player;
import entity.Skill;
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
	private static final String[] BLOCK = { "Attacker Down", "Both Down", "Pushed", "Pushed", "Defender Stumbles",
			                                "Defender Down" }; // pushes repeated as appears twice on block dice
	
	private Game game;
	private int half;
	private String phase;
	private TeamInGame activeTeam;
    private PlayerInGame activePlayer;
	private TeamInGame team1;
	private TeamInGame team2;
	private Tile[][] pitch;
	private boolean waitingForPlayers;
	private Queue<Runnable> queue;
	private Tile ballToScatter;

	public GameService(Game game) {
		this.game = game;
		team1 = new TeamInGame(game.getTeam1());
		team2 = new TeamInGame(game.getTeam2());
		queue = new LinkedList<>();
		activePlayer = null;
		ballToScatter = null;
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

	public void kickOff(TeamInGame kicking) {
		// placeholder
		// check if KO'd players can return
		// team setup, starts with kicking team
		// choose target to kick to. Must be in opponent's half
		// roll on kick off table
		// kick takes place, ball scatters
		// receiving team's turn
	}

	public void endOfHalf() {
		// placeholder
		// check if end of game & if winner
		// if not start new half or extra time
		// if extra time and no winner, go to penalty shoot out
	}
	
	public void newHalf() {
		// placeholder
		// new kickoff with team that received start of last half
		// team re-rolls reset
		// some inducements may come into play here
	}
	
	public void endTurn() { // may be additional steps or user actions at end of turn
		activePlayer = null;
		activeTeam.endTurn();
		activeTeam = (activeTeam == team1 ? team2 : team1);
		if(activeTeam.getTurn()==8) {
			endOfHalf();
		} else {
			activeTeam.incrementTurn();
			newTurn();
		}
	}
	
	public void newTurn() {
		activeTeam.newTurn(); // reset players on pitch (able to move/ act)
	}

	public void showPossibleMovement(PlayerInGame p) {
		if(p != activePlayer && p.getTeamIG() == activeTeam) { // when new player selected will become the active player
			if(activePlayer.getActedThisTurn() == true && p.getActionOver() == false) { // if active player has already acted this turn, deselecting them ends their action 
				endOfAction(activePlayer);
				activePlayer = p;
			}
		}
		resetTiles();
		Tile position = p.getTile();
		int cost = 0;
		if (p.getStatus().equals("prone")) {
			position.setCostToReach(3);
			cost = 3;
		}
		searchNeighbours(p, position, cost);
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
				t.setCostToReach(p.getStatus().equals("prone") ? 3 : 0);
			}
		}
	}

	// An A star algorithm for Player to get from a to b, favouring avoiding tackle
	// zones and going for it
	public List<Tile> getOptimisedPath(PlayerInGame p, int[] goal) {
		actionCheck(p);
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
		origin.setMovementUsed(p.getStatus().equals("prone") ? 3 : 0);
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
		int standingCost = 0;
		if (p.getStatus().equals("prone")) {
			standingCost = 3;
		}
		for (int i = 0; i < route.size(); i++) {
			Tile t = route.get(i);
			System.out.print("\n" + t.getPosition()[0] + " " + t.getPosition()[1]);
			if (i == 0 && standingCost > 0) {
				System.out.print(" Stand Up" + (p.getRemainingMA() < 3 ? " 4+" : ""));
			}
			System.out.print(i + standingCost > p.getRemainingMA() && standingCost == 0 ? " Going For It: 2+" : "");
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
	    actionCheck(p);
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
		for (Tile t : forReset) { 
			t.setPlayer(null);
		}
		origin.addPlayer(p);
		// queue.add(() -> getRouteWithWaypoints((PlayerInGame) p, waypoints, goal));
		return totalRoute;
	}

	public void blitzAction(PlayerInGame attacker, int[][] waypoints, int[] goal, boolean followUp) {
		actionCheck(attacker);
		if(attacker.getTeamIG().hasBlitzed()) {
			throw new IllegalArgumentException("Can only attempt blitz once per turn");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		PlayerInGame defender = pitch[goal[0]][goal[1]].getPlayer();
		attacker.getTeamIG().setBlitzed(true); // counts as blitzed even if movement fails, etc.
		movePlayerRouteAction(attacker, route);
		if (attacker.getStatus().equals("standing")) { // only if movement was successful
			blockAction(attacker, defender, followUp);
			if (attacker.getStatus() == "standing") {
				attacker.setActionOver(false); // player blitzing can move, etc. after
			}
		}
	}

	public void calculateBlitz(PlayerInGame attacker, int[][] waypoints, int[] goal) {
		actionCheck(attacker);
		if(attacker.getTeamIG().hasBlitzed()) {
			throw new IllegalArgumentException("Can only attempt blitz once per turn");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		PlayerInGame opponent = pitch[goal[0]][goal[1]].getPlayer();
		showTravelPath(route);
		int[] block = calculateBlock(attacker, route.get(route.size() - 1), opponent);
		System.out.println();
		System.out.println("Blitz: " + block[0] + " dice, " + (block[1] == attacker.getTeam() ? "attacker" : "defender")
				+ " chooses");
	}
	
	public List<Tile> calculateBlitzRoute(PlayerInGame attacker, int[][]waypoints, int[]goal){
		Tile target = pitch[goal[0]][goal[1]];
		if (!target.containsPlayer() || target.getPlayer().getTeam() == attacker.getTeam()) {
			throw new IllegalArgumentException("No opponent in target square");
		}
		PlayerInGame opponent = target.getPlayer();
		target.removePlayer(); // temporarily remove opponent so can calculate best route there (blitz action
								// uses 1 movement)
		List<Tile> route;
		if (waypoints != null) {
			route = getRouteWithWaypoints(attacker, waypoints, goal);
		} else {
			route = getOptimisedPath(attacker, goal);
		}
		target = pitch[goal[0]][goal[1]];
		//System.out.println(target);
		route.remove(route.size() - 1); // remove movement to opponent's square
		target.addPlayer(opponent);
		return route;
	}
	
	public void foulAction(PlayerInGame attacker, int[][]waypoints, int[]goal) {
		actionCheck(attacker);
		if(attacker.getTeamIG().hasFouled()) {
			throw new IllegalArgumentException("Can only attempt foul once per turn");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		PlayerInGame defender = pitch[goal[0]][goal[1]].getPlayer();
		if(defender.getTeam() == attacker.getTeam()) {
			throw new IllegalArgumentException("Can't foul player on same team");
		}
		attacker.getTeamIG().setFouled(true);
		movePlayerRouteAction(attacker, route);
		boolean refereeSees = false;
		if(attacker.getStatus() == "standing") {
			System.out.println(attacker.getName() + " fouls " + defender.getName());
			int[] rolls = diceRoller(2, 6);
			if(rolls[0] == rolls[1]) { 
				refereeSees = true;
			}
			int total = rolls[0] + rolls[1];
			if (total > defender.getAV()) {
				System.out.println(defender.getName() + "'s armour was broken");
				rolls = diceRoller(2, 6);
				if(rolls[0] == rolls[1]) { 
					refereeSees = true;
				}
				total = rolls[0] + rolls[1];
				if (total <= 7) {
					System.out.println(defender.getName() + " is stunned");
					defender.setStatus("stunned");
				} else {
					// possibility to use apothecary, etc. here
					defender.getTile().removePlayer();
					if (total <= 9) {
						System.out.println(defender.getName() + " is KO'd");
						defender.setStatus("KO");
						defender.getTeamIG().addToDugout(defender);
					} else {
						System.out.println(defender.getName() + " is injured");
						defender.setStatus("injured");
						defender.getTeamIG().addToInjured(defender);
					}
				}
			} else {
				System.out.println(defender.getName() + "'s armour held");
			}
		}
		if(refereeSees == true) {
			sendOff(attacker);
		} else {
			endOfAction(attacker);
		}
	}
	
	public void calculateFoul(PlayerInGame attacker, int[][]waypoints, int[]goal) {
		actionCheck(attacker);
		if(attacker.getTeamIG().hasFouled()) {
			throw new IllegalArgumentException("Can only attempt foul once per turn");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		PlayerInGame defender = pitch[goal[0]][goal[1]].getPlayer();
		if(defender.getStatus().contentEquals("standing")) {
			throw new IllegalArgumentException("Can only foul a player on the ground");
		}
		showTravelPath(route);
		System.out.println();
		int[] assists = calculateAssists(attacker, defender);
		int modifier = assists[0] - assists[1];
		if(modifier == 0) {
			System.out.println("No armour roll modifier");
		} else {
		  System.out.println((modifier < 0 ? "" : "+") + modifier + " to armour roll");
		}
	}
	
	public void sendOff(PlayerInGame p) {
		System.out.println("Referee saw " + p.getName() + " commiting a foul!");
		System.out.println(p.getName() + " is sent off for the rest of the game.");
		Tile location = p.getTile();
		location.removePlayer();
		p.getTeamIG().addToDungeon(p);
		if(p.hasBall()) {
			p.setHasBall(false);
			scatterBall(location, 1);
		}
	}
	

	public void movePlayerRouteAction(PlayerInGame p, List<Tile> route) {
		actionCheck(p);
		addTackleZones(p);
		checkRouteValid(p, route);
		p.setActedThisTurn(true);
		if (p.getStatus().equals("prone")) {
			if (!standUpAction(p)) {
				return;
			}
		}
		route.remove(0);
		for (Tile t : route) {
			Tile tempT = p.getTile();
			t.addPlayer(p);
			tempT.setPlayer(null);
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
			System.out.println(p.getName() + " moved to: " + t.getPosition()[0] + " " + t.getPosition()[1]);
			if (t.containsBall()) {
				if (!pickUpBallAction(p)) {
					return;
				}
			}
			if (p.hasBall()) { // checking if touchdown
				if ((t.getPosition()[0] == 0 && p.getTeam() == team2.getId())
						|| t.getPosition()[0] == 25 && p.getTeamIG() == team1) {
					touchdown(p);
				}
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
				p.setRemainingMA(startingMA);
				throw new IllegalArgumentException("Not enough movement to reach destination");
			}
		}
		p.setRemainingMA(startingMA);
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

	public void blockAction(PlayerInGame attacker, PlayerInGame defender, boolean followUp) {
		actionCheck(attacker);
		int[] dice = calculateBlock(attacker, attacker.getTile(), defender);
		System.out.println(attacker.getName() + " blocks " + defender.getName());
		int[] rolled = diceRoller(dice[0], 6);
		for (int i : rolled) {
			System.out.println("Rolled " + BLOCK[i - 1]);
		}
		rerollCheck();
		int result = rolled[getBlockChoice(rolled, dice[1])] - 1; // get choice of dice from stronger player's user
		System.out.println("Result: " + BLOCK[result]);
		if (result == 0) { // attacker down
			knockDown(attacker);
		} else if (result == 1) { // both down
			if (defender.hasSkill("Block")) {
				System.out.println(defender.getName() + " used block skill");
			} else {
				knockDown(defender);
			}
			if (attacker.hasSkill("Block")) {
				System.out.println(attacker.getName() + " used block skill");
			} else {
				knockDown(attacker);
			}
		} else if (result >= 2) { // push: 2 and 3
			Tile follow = defender.getTile();
			pushAction(attacker, defender, false);
			if (followUp == true) {
				followUp(attacker, follow);
			}
			if (result == 4 && !defender.hasSkill("dodge") || // defender stumbles
					result == 5) { // defender down
				knockDown(defender);
			}
			if (ballToScatter != null) {
				scatterBall(ballToScatter, 1);
				ballToScatter = null;
			}
		}
		endOfAction(attacker);
	}
	
	// result: first element is dice to roll, second element id of team (user) to
	// choose result
	public int[] calculateBlock(PlayerInGame attacker, Tile from, PlayerInGame defender) {
		actionCheck(attacker);
		if (!from.getNeighbours().contains(defender.getTile())) {
			throw new IllegalArgumentException("Can only block an adjacent player");
		}
		if (attacker.getTeam() == defender.getTeam()) {
			throw new IllegalArgumentException("Cannot block player on same team");
		}
		if(defender.getStatus() != "standing") {
			throw new IllegalArgumentException("Cannot block a player on the ground");
		}
		int[] assists = calculateAssists(attacker, defender);
		int attStr = attacker.getST() + assists[0];
		int defStr = defender.getST() + assists[1];
		int strongerTeam = attStr >= defStr ? attacker.getTeam() : defender.getTeam();
		int dice = 1;
		if (attStr >= defStr * 2 || defStr >= attStr * 2)
			dice = 3;
		else if (attStr > defStr || defStr > attStr)
			dice = 2;
		return new int[] { dice, strongerTeam };
	}

	public void pushAction(PlayerInGame attacker, PlayerInGame defender, boolean secondary) {
		List<Tile> push = calculatePushOptions(attacker, defender);
		if (push.isEmpty()) {
			pushOffPitch(defender);
		} else {
			Tile pushChoice = push.get(getPushChoice(push,
					defender.hasSkill("Side step") && !secondary ? defender.getTeamIG() : activeTeam));
			if (pushChoice.containsPlayer()) {
				pushAction(defender, pushChoice.getPlayer(), true);
			} else {
				Tile origin = defender.getTile();
				pushChoice.addPlayer(defender);
				origin.removePlayer();
				System.out.println(defender.getName() + " is pushed back to " + pushChoice.getPosition()[0] + " "
						+ pushChoice.getPosition()[1]);
				if (secondary == true) {
					followUp(attacker, origin);
				}
				if (pushChoice.containsBall()) {
					ballToScatter = pushChoice; // scatter needs to happen after follow up and knockdown
				}
			}
		}
	}

	public int getPushChoice(List<Tile> options, TeamInGame team) {
		// placeholder for handling getting push direction choice from relevant user
		return 0;
	}

	public List<Tile> calculatePushOptions(PlayerInGame attacker, PlayerInGame defender) {
		int xOrigin = attacker.getTile().getPosition()[0];
		int yOrigin = attacker.getTile().getPosition()[1];
		List<Tile> options = new ArrayList<>();
		List<Tile> noEmptyOptions = new ArrayList<>(); // for if all push squares have players in
		for (Tile t : defender.getTile().getNeighbours()) {
			int tx = t.getPosition()[0];
			int ty = t.getPosition()[1];
			if (Math.abs(tx - xOrigin) + // corner push
					Math.abs(ty - yOrigin) > 2 || Math.abs(tx - xOrigin) == 2 && // push from above or below
							Math.abs(xOrigin - defender.getTile().getPosition()[0]) == 1
							&& Math.abs(yOrigin - defender.getTile().getPosition()[1]) == 0
					|| Math.abs(ty - yOrigin) == 2 && // push from left or right
							Math.abs(yOrigin - defender.getTile().getPosition()[1]) == 1
							&& Math.abs(xOrigin - defender.getTile().getPosition()[0]) == 0) {
				noEmptyOptions.add(t);
				if (!t.containsPlayer()) {
					options.add(t);
				}
			}
		}
		return options.size() > 0 ? options : noEmptyOptions;
	}

	public void pushOffPitch(PlayerInGame p) {
		System.out.println(p.getName() + " was pushed into the crowd and gets beaten!");
		injuryRoll(p);
		if (p.getStatus() == "stunned") {
			System.out.println(p.getName() + " was put back in reserves");
			p.setStatus("standing");
			p.getTile().removePlayer();
			p.getTeamIG().addToReserves(p);
		}
		if (p.hasBall()) {
			p.setHasBall(false);
			scatterBall(p.getTile(), 1);
		}
	}

	public void followUp(PlayerInGame p, Tile to) {
		p.getTile().removePlayer();
		to.addPlayer(p);
		System.out.println(p.getName() + " follows up to " + to.getPosition()[0] + " " + to.getPosition()[1]);
	}

	public int getBlockChoice(int[] dice, int team) {
		// placeholder
		return 0;
	}

	public void touchdown(PlayerInGame p) {
		System.out.println(p.getName() + " scored a touchdown!");
		TeamInGame team = p.getTeamIG();
		TeamInGame tg = null;
		if (team == team1) {
			game.setTeam1Score(game.getTeam1Score() + 1);
			tg = team1;
			tg.incrementTurn();
		} else {
			game.setTeam2Score(game.getTeam2Score() + 1);
			tg = team2;
			tg.incrementTurn();
		}
		if (team1.getTurn() > 8 || team2.getTurn() > 8) {
			endOfHalf();
		} else {
			kickOff(tg);
		}
	}

	public void knockDown(PlayerInGame p) {
		p.setProne();
		Tile location = p.getTile();
		int armour = p.getAV();
		int[] rolls = diceRoller(2, 6);
		int total = rolls[0] + rolls[1];
		if (total > armour) {
			System.out.println(p.getName() + "'s armour was broken");
			injuryRoll(p);
		} else {
			System.out.println(p.getName() + "'s armour held");
		}
		if (p.hasBall()) {
			p.setHasBall(false);
			scatterBall(location, 1);
		}
	}

	public void injuryRoll(PlayerInGame p) {
		int[] rolls = diceRoller(2, 6);
		int total = rolls[0] + rolls[1];
		if (total <= 7) {
			System.out.println(p.getName() + " is stunned");
			p.setStatus("stunned");
		} else {
			// possibility to use apothecary, etc. here
			if (total <= 9) {
				System.out.println(p.getName() + " is KO'd");
				p.setStatus("KO");
				p.getTeamIG().addToDugout(p);
			} else {
				System.out.println(p.getName() + " is injured");
				p.setStatus("injured");
				p.getTeamIG().addToInjured(p);
			}
			p.getTile().removePlayer();
		}
	}

	public boolean standUpAction(PlayerInGame player) {
		if (player.getStatus() != "prone") {
			throw new IllegalArgumentException("Can't stand up a player that isn't prone");
		}
		if (player.getRemainingMA() < 3) {
			System.out.println(player.getRemainingMA());
			System.out.println(player.getName() + "tries to stand up.");
			int rollResult = diceRoller(1, 6)[0];
			System.out.println("Needs a roll of 4+. Rolled " + rollResult);
			if (rollResult < 4) {
				System.out.println(player.getName() + " tried, but couldn't stand up");
				return false;
			}
		}
		player.setStatus("standing");
		player.setRemainingMA(player.getRemainingMA() - 3);
		System.out.println(player.getName() + " stood up");
		return true;
	}

	public void rerollCheck() {
		// placeholder
	}

	// if times > 1 cannot try to catch until final scatter
	public void scatterBall(Tile origin, int times) {
		origin.setContainsBall(false);
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
		int roll = diceRoller(1, 6)[0];
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

	public boolean pickUpBallAction(PlayerInGame player) {
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
			return true;
		} else {
			System.out.println(player.getName() + " failed to pick up the ball!");
			rerollCheck();
			scatterBall(player.getTile(), 1);
			return false;
		}
	}

	public void passBallAction(PlayerInGame thrower, Tile target) {
		actionCheck(thrower);
		if (!thrower.hasBall()) {
			throw new IllegalArgumentException("Player doesn't have the ball");
		}
		if(thrower.getTeamIG().hasPassed()) {
			throw new IllegalArgumentException("Can only attempt pass once per turn");
		}
		List<Tile> path = calculateThrowTiles(thrower, thrower.getTile(), target);
		thrower.getTeamIG().setPassed(true); // counts as pass action for turn even if fails
		List<PlayerInGame> interceptors = calculatePossibleInterceptors(path, thrower);
		PlayerInGame interceptor = null;
		if (interceptors.size() > 1) {
			requestInterceptor();
		} else if (interceptors.size() == 1) {
			interceptor = interceptors.get(0);
		}
		int needed = calculateThrow(thrower, thrower.getTile(), target);
		int roll = diceRoller(1, 6)[0];
		System.out.println(thrower.getName() + " tries to throw the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll == 1) {
			System.out.println(thrower.getName() + " fumbled the ball!");
			rerollCheck();
			scatterBall(thrower.getTile(), 1);
		} else {
			if (interceptor != null) {
				if (interceptBallAction(interceptor)) {
					return;
				}
			}
			if (roll >= needed) {
				System.out.println(thrower.getName() + " threw the ball accurately!");
				thrower.setHasBall(false);
				if (target.containsPlayer()) {
					catchBallAction(target.getPlayer(), true);
				} else {
					target.setContainsBall(true);
				}
			} else {
				System.out.println(thrower.getName() + " threw the ball badly");
				rerollCheck();
				scatterBall(target, 3);
			}
		}
		endOfAction(thrower);
	}

	public void handOffBallAction(PlayerInGame player, Tile target) {
		actionCheck(player);
		if (!player.hasBall()) {
			throw new IllegalArgumentException("Player doesn't have the ball");
		}
		if (!target.containsPlayer()) {
			throw new IllegalArgumentException("Must hand off ball to a player");
		}
		if(player.getTeamIG().hasHandedOff()) {
			throw new IllegalArgumentException("Can only hand off ball once per turn");
		}
		player.getTeamIG().setHandedOff(true);
		System.out.println(player.getName() + " hands off the ball to " + target.getPlayer().getName());
		player.setHasBall(false);
		catchBallAction(target.getPlayer(), true);
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

	public int calculateHandOff(PlayerInGame p, Tile from, Tile target) {
		if (!target.containsPlayer()) {
			throw new IllegalArgumentException("Must hand off ball to a player");
		}
		return calculateCatch(target.getPlayer(), true);
	}

	public int calculateThrow(PlayerInGame thrower, Tile from, Tile target) {
		actionCheck(thrower);
		if(thrower.getTeamIG().hasPassed()) {
			throw new IllegalArgumentException("Can only attempt pass once per turn");
		}
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
		opponents = activePlayer.getTeamIG() == team1 ? team2.getPlayersOnPitch()
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
	
	public void endOfAction(PlayerInGame player) { // will involve informing front end
		player.setActionOver(true);
	}

	public static int[] diceRoller(int quantity, int number) {
		Random rand = new Random();
		int[] result = new int[quantity];
		for (int i = 0; i < quantity; i++) {
			result[i] = rand.nextInt(number) + 1;
		}
		return result;
	}
	
	public void actionCheck(PlayerInGame p) {
		if(p.getTeamIG() != activeTeam) {
			throw new IllegalArgumentException("Not that player's turn");
		}
		if(p.getActionOver() == true) {
			throw new IllegalArgumentException("That player's action has finished for this turn");
		}
		if(p.getStatus() == "stunned") {
			throw new IllegalArgumentException("A stunned player cannot act");
		}
		
	}
	
	public void setActiveTeam(TeamInGame team) {
		activeTeam = team;
	}

	public static void main(String[] args) {
		Player p = new Player();
		p.setName("Billy");
		p.setMA(4);
		p.setAG(6);
		p.setTeam(1);
		p.setST(9);
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
		p3.setAV(11);
		Skill block = new Skill("Block", "blocking is fun", "block");
		List<Skill> skills = new ArrayList<>();
		skills.add(block);
		p3.setSkills(skills);
		Player p4 = new Player();
		p4.setName("Sarah");
		p4.setMA(3);
		p4.setAG(4);
		p4.setTeam(1);
		p4.setST(3);
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
		gs.pitch[6][6].addPlayer(team1Players.get(0));
		gs.pitch[5][3].addPlayer(team1Players.get(1));
		gs.pitch[7][5].addPlayer(team2Players.get(0));
		gs.pitch[7][7].addPlayer(team2Players.get(1));
		 team2Players.get(1).setStatus("prone");
         gs.setActiveTeam(gs.team1);
        
        // gs.team1.setFouled(true);
		//Tile ballTile = gs.pitch[24][7];
		//ballTile.setContainsBall(true);
		 team1Players.get(0).setHasBall(true);
		// gs.pickUpBallAction(team1Players.get(0));
		// gs.handOffBallAction(team1Players.get(0), gs.pitch[7][8]);
		// gs.throwBallAction(team1Players.get(0), gs.pitch[3][3]);

		// List<Tile> squares = gs.calculateThrowTiles(team1Players.get(0),
		// team1Players.get(0).getTile(), gs.pitch[3][3]);
//		for(Tile t : squares) {
//			System.out.println(t.getPosition()[0] + " " + t.getPosition()[1]);
//		}
		// List<PlayerInGame> interceptors = gs.calculatePossibleInterceptors(squares,
		// team1Players.get(0));
		// System.out.println(interceptors.size());

		// gs.showPossibleMovement(team1Players.get(0));
		int[] goal = { 7, 7 };
		int[][] waypoints = { { 5, 6 } };
		// int[][] waypoints = null;
		//gs.blitzAction(team1Players.get(0), waypoints, goal, true);
		gs.foulAction(team1Players.get(0), waypoints, goal);
		
		 gs.endTurn();
		 gs.endTurn();
		 System.out.println(gs.activeTeam.getName());
		 gs.foulAction(team1Players.get(0), waypoints, goal);
		 
		// List<Tile> pushes = gs.calculatePushOptions(team1Players.get(0),
		// team2Players.get(0));
		// gs.blockAction(team1Players.get(0), team2Players.get(0), false);
		// List<Tile> route = gs.getOptimisedPath(team1Players.get(0), goal);
		// gs.showTravelPath(route);
		// gs.movePlayerRouteAction(team1Players.get(0), route);
		// gs.getOptimisedPath((PlayerInGame) p, goal);
		// int[][] waypoints = { { 5, 6 }, { 7, 7 } };
		// List<Tile> route = gs.getRouteWithWaypoints(team1Players.get(0), waypoints,
		// goal);
		// gs.showTravelPath(route);
//		for (Tile t : pushes) {
//			System.out.println(t.getPosition()[0] + " " + t.getPosition()[1]);
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
