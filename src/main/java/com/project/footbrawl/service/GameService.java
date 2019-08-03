package com.project.footbrawl.service;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.project.footbrawl.entity.Game;
import com.project.footbrawl.instance.PlayerInGame;
import com.project.footbrawl.instance.TeamInGame;
import com.project.footbrawl.instance.Tile;
import com.project.footbrawl.instance.jsonTile;

// controls a game's logic and progress
// future: contain DTO for database interactions
@Service
@Scope("prototype")
public class GameService {

	@Autowired
	MessageSendingService sender;

	private static List<Integer> diceRolls = new ArrayList<>(
			Arrays.asList(new Integer[] { 1, 6, 1, 6, 6, 6, 6, 1, 1, 6, 6, 4, 1, 6, 6, 6, 6, 6, 6, 6, 6, 6 }));
	private static boolean testing = true;

	// needed for finding neighbouring tiles
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
	private TeamInGame lastKickOff;
	private String phase;
	private TeamInGame activeTeam;
	PlayerInGame activePlayer;
	TeamInGame team1;
	TeamInGame team2;
	Tile[][] pitch;
	private boolean waitingForPlayers;
	private boolean kickingSetupDone;
	private boolean receivingSetupDone;
	private LinkedList<Runnable> taskQueue;
	private BlockingQueue<Boolean> runnableResults;
	private int[][] runnableLocation;
	private int[] ballLocation;
	private Tile ballToScatter;
	private boolean inPassOrHandOff; // need to track to ensure max one turnover, as throw can result in complex
	// chain of events
	private String rollType;
	private int rollNeeded;
	private List<Integer> rolled;
	private String rollResult;
	private String[] awaitingReroll; // Y/N, Action relates to, player relates to
	private int actionsNeeded;
	private boolean routeSaved;
	private List<String> rerollOptions;
	private boolean inTurnover;
	private Runnable blitz;
	private PlayerInGame interceptor;

//	public GameService(Game game) {
//		this.game = game;
//		team1 = new TeamInGame(game.getTeam1());
//		team2 = new TeamInGame(game.getTeam2());
//		queue = new LinkedList<>();
//		activePlayer = null;
//		ballToScatter = null;
//		pitch = new Tile[26][15];
//		for (int row = 0; row < 26; row++) {
//			for (int column = 0; column < 15; column++) {
//				pitch[row][column] = new Tile(row, column);
//			}
//		}
//		setTileNeighbours(); // doing it once and saving in Tile objects saves repeated computations
//	}

	public GameService(MessageSendingService sender) {
		this.sender = sender;
	}

	public void setGame(Game game) {
		this.game = game;
		team1 = new TeamInGame(game.getTeam1());
		team2 = new TeamInGame(game.getTeam2());
		taskQueue = new LinkedList<>();
		rolled = new ArrayList<>();
		runnableResults = new LinkedBlockingQueue<>();
		actionsNeeded = 0;
		activePlayer = null;
		ballToScatter = null;
		rerollOptions = new ArrayList<>();
		inTurnover = false;
		pitch = new Tile[26][15];
		for (int row = 0; row < 26; row++) {
			for (int column = 0; column < 15; column++) {
				pitch[row][column] = new Tile(row, column);
			}
		}
		setTileNeighbours(); // doing it once and saving in Tile objects saves repeated computations
	}

	public int getGameId() {
		return game.getId();
	}

	public List<Tile> getNeighbours(Tile t) {
		List<Tile> neighbours = new ArrayList<Tile>();
		int row = t.getLocation()[0];
		int column = t.getLocation()[1];
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

	public void startGame() {
		half = 1;
		// send both player details of teams
		coinToss();
	}

	public void coinToss() {
		TeamInGame winner;
		TeamInGame loser;
		if (diceRoller(1, 2)[0] == 1) {
			winner = team1;
			loser = team2;
		} else {
			winner = team2;
			loser = team1;
		}
		System.out.println(winner.getName() + " won the coin toss!");
		TeamInGame kicking = chooseKickOff(winner) ? winner : loser;
		lastKickOff = kicking;
		kickOff(kicking);
	}

	public boolean chooseKickOff(TeamInGame chooser) {
		// placeholder
		// ask relevant user if they want to kick (true) or receive (false) to start
		return true;
	}

	public void kickOff(TeamInGame kicking) {
		if (!(half == 1 && game.getTeam1Score() == 0 && game.getTeam2Score() == 0)) { // no KO's if first kickoff
			checkKOs();
		}
		team1.newKickOff();
		team2.newKickOff();
		if (team1.getReserves().size() == 0 || team2.getReserves().size() == 0) {
			TeamInGame emptyTeam;
			TeamInGame otherTeam;
			if (team1.getReserves().size() == 0) {
				emptyTeam = team1;
				otherTeam = team2;
			} else {
				emptyTeam = team2;
				otherTeam = team1;
			}
			System.out.println(emptyTeam.getName() + " has no players to put on pitch.");
			System.out.println(otherTeam.getName() + " is awarded an extra touchdown.");
			System.out
					.println("Teams wait 2 turns to see if any of " + emptyTeam.getName() + "'s KO'd players wake up.");
			team1.incrementTurn();
			team1.incrementTurn();
			team2.incrementTurn();
			team2.incrementTurn();
			if (team1.getTurn() > 8 || team2.getTurn() > 8) {
				newHalf();
			} else {
				kickOff(kicking);
			}
		}
		activeTeam = kicking;
		getTeamSetup(kicking);
		// team setup, starts with kicking team
		// choose target to kick to. Must be in opponent's half
		// roll on kick off table (low priority)
		// kick takes place, ball scatters
		// receiving team's turn
	}

	public void getTeamSetup(TeamInGame team) {
		// placeholder
		// ask team to setup
	}

	public void playerPlacement(PlayerInGame player, int[] position) {
		if (player.getTeamIG() != activeTeam) {
			throw new IllegalArgumentException("Not your setup phase");
		}
		Tile target = pitch[position[0]][position[1]];
		if (checkValidPlacement(player, target)) {
			if (target.containsPlayer()) {
				PlayerInGame tempP = target.getPlayer();
				if (player.getTile() != null) { // swap if both already on pitch
					Tile tempT = player.getTile();
					tempT.addPlayer(tempP);
				} else {
					player.getTeamIG().addToReserves(tempP); // otherwise put existing player back in reserves
				}
				target.addPlayer(player);
				player.getTeamIG().addPlayerOnPitch(player);
			}
		}
	}

	public void endTeamSetup(TeamInGame team) {
		if (checkTeamSetupValid(team)) {
			activeTeam = activeTeam == team1 ? team2 : team1;
			if (kickingSetupDone == false) {
				kickingSetupDone = true;
				getTeamSetup(activeTeam);
			} else {
				receivingSetupDone = true;
				getKickChoice(activeTeam);
			}

		}
	}

	public void getKickChoice(TeamInGame kicking) {
		// placeholder
		// ask relevant user where they want to kick ball to
	}

	public boolean checkTeamSetupValid(TeamInGame team) {
		if (team.getPlayersOnPitch().size() < 11 && !team.getReserves().isEmpty()) {
			throw new IllegalArgumentException("Must place 11 players on pitch, or as many as you can");
		}
		int wideZone1 = 0;
		int wideZone2 = 0;
		int scrimmage = 0;
		for (PlayerInGame p : team.getPlayersOnPitch()) {
			if (p.getTile().getLocation()[1] >= 0 && p.getTile().getLocation()[1] <= 3) {
				wideZone1++;
			} else if (p.getTile().getLocation()[1] >= 10 && p.getTile().getLocation()[1] <= 14) {
				wideZone2++;
			} else if (team == team1 && p.getTile().getLocation()[0] == 12
					|| team == team2 && p.getTile().getLocation()[0] == 13) {
				scrimmage++;
			}
		}
		if (wideZone1 >= 2 || wideZone2 >= 2) {
			throw new IllegalArgumentException("Cannot have more than 2 players in a widezone");
		}
		if (scrimmage < 3 && team.getPlayersOnPitch().size() + team.getReserves().size() >= 3) {
			throw new IllegalArgumentException(
					"Must have at least 3 players on line of scrimmage, or as many as you can");
		}
		return true;
	}

	public void playerPlacementRemove(PlayerInGame player) {
		if (player.getTeamIG() != activeTeam) {
			throw new IllegalArgumentException("Not your setup phase");
		}
		player.getTeamIG().addToReserves(player);
	}

	public boolean checkValidPlacement(PlayerInGame player, Tile target) {
		TeamInGame team = player.getTeamIG();
		if (!target.containsPlayer()) {
			if (team.getPlayersOnPitch().size() >= 11) {
				throw new IllegalArgumentException("Cannot have more than 11 players on the pitch");
			}
			if (target.getLocation()[1] >= 0 && target.getLocation()[1] <= 3) {
				int wideZone1 = 0;
				for (PlayerInGame p : team.getPlayersOnPitch()) {
					if (p.getTile().getLocation()[1] >= 0 && p.getTile().getLocation()[1] <= 3) {
						wideZone1++;
					}
				}
				if (wideZone1 >= 2) {
					throw new IllegalArgumentException("Cannot have more than 2 players in a widezone");
				}
			} else if (target.getLocation()[1] >= 10 && target.getLocation()[1] <= 14) {
				int wideZone2 = 0;
				for (PlayerInGame p : team.getPlayersOnPitch()) {
					if (p.getTile().getLocation()[1] >= 10 && p.getTile().getLocation()[1] <= 14) {
						wideZone2++;
					}
				}
				if (wideZone2 >= 2) {
					throw new IllegalArgumentException("Cannot have more than 2 players in a widezone");
				}
			}
			if (team == team1 && target.getLocation()[0] > 12 || team == team2 && target.getLocation()[0] < 13) {
				throw new IllegalArgumentException("Must be placed in your half of the pitch");
			}
		}
		return true; // if contains a player (on right side), must be valid for other checks
	}

	public void checkKOs() {
		System.out.println("Checking if KO'd players wake up");
		if (team1.getDugout().isEmpty()) {
			System.out.println("Team 1 has no KO'd players");
		} else {
			for (PlayerInGame p : team1.getDugout()) {
				if (diceRoller(1, 6)[0] >= 4) {
					team1.addToReserves(p);
					team1.removeFromDugout(p);
					System.out.println(p.getName() + " wakes up and joins the rest of his team");
				} else {
					System.out.println(p.getName() + " is still KO'd");
				}
			}
		}
		if (team2.getDugout().isEmpty()) {
			System.out.println("Team 1 has no KO'd players");
		} else {
			for (PlayerInGame p : team2.getDugout()) {
				if (diceRoller(1, 6)[0] >= 4) {
					team1.addToReserves(p);
					team1.removeFromDugout(p);
					System.out.println(p.getName() + " wakes up and joins the rest of his team");
				} else {
					System.out.println(p.getName() + " is still KO'd");
				}
			}
		}
	}

	public void kickBall(int[] target) {
		phase = "kick";
		Tile goal = pitch[target[0]][target[1]];
		if (activeTeam == team2 && goal.getLocation()[0] > 12 || activeTeam == team1 && goal.getLocation()[0] < 13) {
			throw new IllegalArgumentException("Must kick to opponent's half of the pitch");
		}
		int value = diceRoller(1, 8)[0];
		int[] direction = ADJACENT[value - 1];
		int[] position = new int[] { goal.getLocation()[0] + direction[0], goal.getLocation()[1] + direction[1] };
		if (position[0] > 0 && position[0] < 26 && position[1] >= 0 && position[1] < 15) {
			goal = pitch[position[0]][position[1]];
			System.out.println("Ball flew to: " + position[0] + " " + position[1]);
			if (activeTeam == team2 && goal.getLocation()[0] > 12
					|| activeTeam == team1 && goal.getLocation()[0] < 13) {
				System.out.println("Ball landed on kicking team's side, so receivers are given the ball");
				getTouchBack(activeTeam == team1 ? team2 : team1);
			}
			if (goal.containsPlayer()) {
				if (goal.getPlayer().isHasTackleZones()) { // will need to make this more specific to catching
					catchBallAction(goal.getPlayer(), false);
				} else {
					scatterBall(goal, 1); // if player can't catch, will scatter again
				}
			} else {
				scatterBall(goal, 1); // will need a message to inform front end of this ball movement
			}
			Tile scatteredTo = ballLocationCheck();
			if (activeTeam == team2 && scatteredTo.getLocation()[0] > 12
					|| activeTeam == team1 && scatteredTo.getLocation()[0] < 13) {
				System.out.println("Ball ended on kicking team's side, so receivers are given the ball");
				getTouchBack(activeTeam == team1 ? team2 : team1);
			}
		}
		phase = "main game";
		activeTeam = (activeTeam == team1 ? team2 : team1);
		activeTeam.incrementTurn();
		newTurn();
	}

	public void getTouchBack(TeamInGame team) {
		// placeholder
		// for relevant user to specify which player to be given ball
	}

	public void endOfHalf() {
		// placeholder
		if (half == 1) {
			newHalf();
		} else if (half == 2) {
			if (game.getTeam1Score() == game.getTeam2Score()) {
				extraTime();
			} else {
				endGame(game.getTeam1Score() > game.getTeam2Score() ? team1 : team2);
			}
		} else if (half == 3) {
			if (game.getTeam1Score() == game.getTeam2Score()) {
				penaltyShootOuts();
			}
		} else {
			endGame(game.getTeam1Score() > game.getTeam2Score() ? team1 : team2);
		}
		// check if end of game & if winner
		// if not start new half or extra time
		// if extra time and no winner, go to penalty shoot out
	}

	public void endGame(TeamInGame winners) {
		System.out.println(winners.getName() + " won the match!");
		// with database, will save result
		// in league will need to update league points
	}

	public void extraTime() {

	}

	public void penaltyShootOuts() {

	}

	public void newHalf() {
		// placeholder
		// team re-rolls reset
		team1.resetRerolls();
		team2.resetRerolls();
		// new kickoff with team that received start of last half
		kickOff(lastKickOff == team1 ? team2 : team1);
		// some inducements may come into play here
	}

	public void turnover() {
		if (inTurnover == false) {
			System.out.println("Turnover");
			inTurnover = true;
			System.out.println(activeTeam.getName() + " suffered a turnover");
			sender.sendTurnover(game.getId(), activeTeam.getId(), activeTeam.getName());
			endTurn();
		}
	}

	// for internal endTurn actions (from within this object)
	public void endTurn() { // may be additional Steps or user actions at end of turn
		awaitingReroll = null;
		inPassOrHandOff = false;
		activePlayer = null;
		activeTeam.endTurn();
		blitz = null;
		activeTeam = (activeTeam == team1 ? team2 : team1);
		if (activeTeam.getTurn() == 8) {
			endOfHalf();
		} else {
			activeTeam.incrementTurn();
			newTurn();
		}
	}

	// for client requests to end turn
	public void endTurn(int team) {
		if (team != activeTeam.getId()) {
			throw new IllegalArgumentException("Not their turn to end");
		} else {
			endTurn();
		}
	}

	public void newTurn() {
		activeTeam.newTurn();// reset players on pitch (able to move/ act)
		taskQueue.clear();
		sender.sendGameStatus(game.getId(), activeTeam.getId(), activeTeam.getName(), team1, team2,
				game.getTeam1Score(), game.getTeam2Score(), ballLocationCheck().getLocation());
	}

	public void showPossibleMovement(int playerId, int[] location, int maUsed, int requester) {
		inTurnover = false;
		if (awaitingReroll != null && awaitingReroll[0] == "Y") {
			throw new IllegalArgumentException("Can't do anything whilst waiting for reroll decision");
		}
		List<jsonTile> squares = new ArrayList<>();
		System.out.println("Determining movement options");
		PlayerInGame p = getPlayerById(playerId);
		int originalMA = p.getRemainingMA();
		if (p.getStatus() != "stunned") {
			if (activePlayer == null) {
				activePlayer = p;
			}
			System.out.println("active : " + activePlayer.getName());
			if (maUsed < p.getMA() + 2 && p.getActionOver() == false) { // don't try to work out if given an impossibly
																		// high
																		// number for movement used
				System.out.println("action not over");
				if (p != activePlayer && p.getTeamIG() == activeTeam) {
					System.out.println(p.getActionOver());
					System.out.println(activePlayer.getActedThisTurn());
					if (activePlayer.getActedThisTurn() == true) { // if active player has
																	// already acted this
																	// turn,
																	// deselecting them ends
																	// their action
						System.out.println("updating activePlayer");
						endOfAction(activePlayer);
						activePlayer = p;
						System.out.println("active player is: " + activePlayer.getName());
					}
				}
				System.out.println("resetting");
				resetTiles();
				System.out.println("reset");
				p.setRemainingMA(originalMA - maUsed);
				Tile position = pitch[location[0]][location[1]];
				int cost = 0;
				if (p.getStatus().equals("prone") && maUsed == 0) {
					position.setCostToReach(3);
					cost = 3;
				}
				searchNeighbours(p, position, cost);
				for (int i = 0; i < 26; i++) {
					for (int j = 0; j < 15; j++) {
						Tile t = pitch[i][j];
						if (t.getCostToReach() != 99) {
							jsonTile jTile = new jsonTile(t);
							if (t.getCostToReach() == 77 && t != position) {
								jTile.setGoingForItRoll(2); // if blizzard this will be 3
							}
							squares.add(jTile);
						}
					}
				}
			}
		}
		p.setRemainingMA(originalMA);
		sender.sendMovementInfoMessage(game.getId(), requester, playerId, squares);
	}

	// breadth first search to determine where can move
	public void searchNeighbours(PlayerInGame p, Tile location, int cost) {
		if (cost == p.getRemainingMA() + 2) {
			return;
		}
		addTackleZones(p);
		for (Tile t : location.getNeighbours()) {
			if (!t.containsPlayer() || t.containsPlayer() && t.getPlayer() == p) {
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
			} else if (t.getLocation() == location.getLocation()) {
				t.setCostToReach(p.getStatus().equals("prone") ? 3 : 0);
			}
		}
	}

	// An A star algorithm for Player to get from a to b, favouring avoiding tackle
	// zones and going for it
	public List<Tile> getOptimisedRoute(int playerId, int[] from, int[] goal) {
		PlayerInGame p = getPlayerById(playerId);
		actionCheck(p);
		addTackleZones(p);
		Tile origin = pitch[from[0]][from[1]];
		// Tile origin = selectedPlayer.getTile();
		Tile target = pitch[goal[0]][goal[1]];
		int MA = p.getRemainingMA();
		System.out.println("optimised remaining MA " + MA);

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

						double predictedDistance = Math.abs((neighbour.getLocation()[0] - target.getLocation()[0]))
								+ Math.abs((neighbour.getLocation()[1] - target.getLocation()[1]));

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

	public List<jsonTile> jsonRoute(List<Tile> route) {
		List<jsonTile> jsonRoute = new ArrayList<>();
		if (route.isEmpty()) {
			return new ArrayList<jsonTile>();
		}
		PlayerInGame p = route.get(0).getPlayer();
		addTackleZones(p);
		int standingCost = 0;
		if (p.getStatus().equals("prone")) {
			standingCost = 3;
		}
		for (int i = 0; i < route.size(); i++) {
			Tile t = route.get(i);
			jsonTile jt = new jsonTile(t);
			System.out.print("\n" + t.getLocation()[0] + " " + t.getLocation()[1]);
			if (i == 0 && standingCost > 0) {
				// System.out.print(" Stand Up" + (p.getRemainingMA() < 3 ? " 4+" : ""));
				jt.setStandUpRoll((p.getRemainingMA() < 3 ? 4 : 0));
			}
			if (i + standingCost > p.getRemainingMA()) {
				jt.setGoingForItRoll(2);
			}
			if (i > 0) {
				if (route.get(i - 1).getTackleZones() != 0) {
					// System.out.print(" Dodge: " + calculateDodge(p, route.get(i - 1)) + "+");
					jt.setDodgeRoll(calculateDodge(p, route.get(i - 1)));
				}
			}
			if (t.containsBall()) {
				System.out.print(" Pick Up Ball: " + calculatePickUpBall(p, t) + "+");
				jt.setPickUpBallRoll(calculatePickUpBall(p, t));
			}
			jsonRoute.add(jt);
			System.out.println(jt.getPosition());
		}
		return jsonRoute;
	}

	public List<Tile> getRouteWithWaypoints(int playerId, List<int[]> waypoints, int[] goal) {
		PlayerInGame p = getPlayerById(playerId);
		actionCheck(p);
		int startingMA = p.getRemainingMA();
		List<Tile> totalRoute = new ArrayList<>();
		Tile origin = p.getTile();
		try {
			for (int[] i : waypoints) {
				totalRoute.addAll(getOptimisedRoute(p.getId(), origin.getLocation(), i));
				origin = totalRoute.get(totalRoute.size() - 1);
				totalRoute.remove(totalRoute.size() - 1); // removes duplicate tiles
				p.setRemainingMA(startingMA - (totalRoute.size()));
				System.out.println("remaining MA: " + p.getRemainingMA());
			}
			totalRoute.addAll(getOptimisedRoute(p.getId(), origin.getLocation(), goal));
		} catch (Exception e) {
			System.out.println("Can't reach here");
			return new ArrayList<Tile>();
		} finally {
			p.setRemainingMA(startingMA);
			// queue.add(() -> getRouteWithWaypoints((PlayerInGame) p, waypoints, goal));
		}
		return totalRoute;
	}

	public void blitzAction(PlayerInGame attacker, PlayerInGame defender, List<int[]> route, boolean followUp) {
		actionCheck(attacker);
		if (attacker.getTeamIG().hasBlitzed()) {
			throw new IllegalArgumentException("Can only attempt blitz once per turn");
		}
		attacker.getTeamIG().setBlitzed(true); // counts as blitzed even if movement fails, etc.
		sender.sendBlitzUsed(game.getId(), attacker.getTeam());
		blitz = new Runnable() {
			@Override
			public void run() {
				if (attacker.getStatus().equals("standing")) { // only if movement was successful
					carryOutBlock(attacker.getId(), defender.getId(), route.get(route.size() - 1), followUp, false,
							attacker.getTeam());
				}
			}
		};
		carryOutRouteAction(attacker.getId(), route, attacker.getTeam());
	}

	public void sendBlitzDetails(Integer player, Integer defender, List<int[]> waypoints, int[] goal, int team) {
		PlayerInGame attacker = getPlayerById(player);
		actionCheck(attacker);
		if (attacker.getTeamIG().hasBlitzed()) {
			throw new IllegalArgumentException("Can only attempt blitz once per turn");
		}
		Tile target = pitch[goal[0]][goal[1]];
		if (target.containsPlayer() == false || target.getPlayer().getTeam() == attacker.getTeam()
				|| !Arrays.equals(target.getLocation(), goal)) {
			throw new IllegalArgumentException("Invalid target");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		List<jsonTile> jRoute = jsonRoute(route);
		PlayerInGame opponent = target.getPlayer();
		int[] block = calculateBlock(attacker, route.get(route.size() - 1), opponent);
		System.out.println();
		System.out.println("Blitz: " + block[0] + " dice, " + (block[1] == attacker.getTeam() ? "attacker" : "defender")
				+ " chooses");
		int routeMACost;
		if (route.isEmpty()) {
			routeMACost = 0;
		} else {
			routeMACost = jRoute.size() - 1 + (jRoute.get(1).getStandUpRoll() != null ? 3 : 0);
		}
		int[][] attLocations = getJsonFriendlyAssists(attacker, opponent);
		int[][] defLocations = getJsonFriendlyAssists(opponent, attacker);
		sender.sendBlitzDetails(game.getId(), attacker.getId(), opponent.getId(),
				route.get(route.size() - 1).getLocation(), opponent.getLocation(), attLocations, defLocations, jRoute,
				routeMACost, block, attacker.getTeam());
	}

	public List<Tile> calculateBlitzRoute(PlayerInGame attacker, List<int[]> waypoints, int[] goal) {
		Tile target = pitch[goal[0]][goal[1]];
		if (!target.containsPlayer() || target.getPlayer().getTeam() == attacker.getTeam()) {
			throw new IllegalArgumentException("No opponent in target square");
		}
		PlayerInGame opponent = target.getPlayer();
		target.removePlayer(); // temporarily remove opponent from tile so can calculate best route there
								// (blitz action
								// uses 1 movement)
		opponent.setTile(target); // but player needs to keep tile to prevent null exception
		List<Tile> route;
		if (waypoints != null) {
			route = getRouteWithWaypoints(attacker.getId(), waypoints, goal);
		} else {
			route = getOptimisedRoute(attacker.getId(), attacker.getLocation(), goal);
		}
		target = pitch[goal[0]][goal[1]];
		if (route.isEmpty()) {
			throw new IllegalArgumentException("Cant reach there");
		}
		route.remove(route.size() - 1); // remove movement to opponent's square
		target.addPlayer(opponent);
		return route;
	}

	public void foulAction(PlayerInGame attacker, List<int[]> waypoints, int[] goal) {
		actionCheck(attacker);
		if (attacker.getTeamIG().hasFouled()) {
			throw new IllegalArgumentException("Can only attempt foul once per turn");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		PlayerInGame defender = pitch[goal[0]][goal[1]].getPlayer();
		if (defender.getTeam() == attacker.getTeam()) {
			throw new IllegalArgumentException("Can't foul player on same team");
		}
		int[] assists = calculateAssists(attacker, defender);
		int modifier = assists[0] - assists[1];
		attacker.getTeamIG().setFouled(true);
		movePlayerRouteAction(attacker, route);
		boolean refereeSees = false;
		if (attacker.getStatus() == "standing") {
			System.out.println(attacker.getName() + " fouls " + defender.getName());
			int[] rolls = diceRoller(2, 6);
			if (rolls[0] == rolls[1]) {
				refereeSees = true;
			}
			int total = rolls[0] + rolls[1] + modifier;
			if (total > defender.getAV()) {
				System.out.println(defender.getName() + "'s armour was broken");
				rolls = diceRoller(2, 6);
				if (rolls[0] == rolls[1]) {
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
		if (refereeSees == true) {
			System.out.println("Referee saw " + attacker.getName() + " commiting a foul!");
			// need to add logic for bribes
			sendOff(attacker);
		} else {
			endOfAction(attacker);
		}
	}

	public void calculateFoul(PlayerInGame attacker, List<int[]> waypoints, int[] goal) {
		actionCheck(attacker);
		if (attacker.getTeamIG().hasFouled()) {
			throw new IllegalArgumentException("Can only attempt foul once per turn");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		PlayerInGame defender = pitch[goal[0]][goal[1]].getPlayer();
		if (defender.getStatus().contentEquals("standing")) {
			throw new IllegalArgumentException("Can only foul a player on the ground");
		}
		System.out.println();
		int[] assists = calculateAssists(attacker, defender);
		int modifier = assists[0] - assists[1];
		if (modifier == 0) {
			System.out.println("No armour roll modifier");
		} else {
			System.out.println((modifier < 0 ? "" : "+") + modifier + " to armour roll");
		}
	}

	public void sendOff(PlayerInGame p) {
		System.out.println(p.getName() + " is sent off for the rest of the game.");
		Tile location = p.getTile();
		location.removePlayer();
		p.getTeamIG().addToDungeon(p);
		if (p.isHasBall()) {
			p.setHasBall(false);
			scatterBall(location, 1);
		}
		turnover();
	}

	private void checkRouteValid(PlayerInGame p, List<Tile> route) {
		if (route.isEmpty()) {
			return;
		}
		List<Tile> tempR = new ArrayList<>(route);
		int startingMA = p.getRemainingMA();
		if (p.getTile() != tempR.get(0)) {
			System.out.println("Player: " + p.getTile().getLocation()[0] + ", " + p.getTile().getLocation()[1]);
			System.out.println("Route Start: " + tempR.get(0).getLocation()[0] + ", " + tempR.get(0).getLocation()[1]);

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
		rollType = "GFI";
		rollNeeded = 2;
		rolled.clear();
		rolled.add(result);
		if (result >= 2) {
			System.out.println(p.getName() + " went for it!");
			rollResult = "success";
			return true;
		} else {
			System.out.println(p.getName() + " went for it and tripped!");
			rollResult = "failed";
			// knockDown(p);
			return false;
		}
	}

	public boolean dodgeAction(PlayerInGame p, Tile from, Tile to) {
		int roll = calculateDodge(p, from);
		int result = diceRoller(1, 6)[0];
		System.out.println("Needed " + roll + "+" + " Rolled: " + result);
		rollType = "DODGE";
		rollNeeded = roll;
		rolled.clear();
		rolled.add(result);
		if (result >= roll) {
			System.out.println(p.getName() + " dodged from " + from.getLocation()[0] + " " + from.getLocation()[1]
					+ " to " + to.getLocation()[0] + " " + to.getLocation()[1] + " with a roll of " + result);
			rollResult = "success";
			return true;
		} else {
			System.out.println(p.getName() + " failed to dodge and was tripped into " + to.getLocation()[0] + " "
					+ to.getLocation()[1]);

			rollResult = "failed";
			// knockDown(p);

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

	public int[] blockAction(PlayerInGame attacker, PlayerInGame defender, boolean followUp) {
		actionCheck(attacker);
		if (attacker.isActedThisTurn() && blitz == null) {
			throw new IllegalArgumentException("Can't act and then block unless blitzing");
		}
		int[] dice = calculateBlock(attacker, attacker.getTile(), defender);
		System.out.println(attacker.getName() + " blocks " + defender.getName());
		int[] rolls = diceRoller(dice[0], 6);
		rollType = "BLOCK";
		rolled.clear();
		for (int i : rolls) {
			System.out.println("Rolled " + BLOCK[i - 1]);
			rolled.add(i);
		}
		rerollOptions = determineRerollOptions("BLOCK", attacker.getId(),
				new int[][] { attacker.getTile().getLocation() });
		if (!rerollOptions.isEmpty()) {
			awaitingReroll = new String[] { "Y", "BLOCK", "" + attacker.getId(), "" + defender.getId() };
		} else {
			awaitingReroll = new String[] { "N", "BLOCK", "" + attacker.getId(), "" + defender.getId() };
		}
		return dice;
	}

	// get choice of dice from stronger player's user
	public void blockChoiceAction(int blockChoice, PlayerInGame attacker, PlayerInGame defender, boolean followUp) {
		int result = blockChoice;
		System.out.println("Result: " + BLOCK[result]);
		if (result == 0) { // attacker down
			knockDown(attacker);
		} else if (result == 1) { // both down
			if (defender.hasSkill("Block")) {
				sender.sendSkillUsed(game.getId(), defender.getId(), defender.getName(), defender.getTeam(), "Block");
				System.out.println(defender.getName() + " used block skill");
			} else {
				knockDown(defender);
			}
			if (attacker.hasSkill("Block")) {
				sender.sendSkillUsed(game.getId(), attacker.getId(), attacker.getName(), attacker.getTeam(), "Block");
				System.out.println(attacker.getName() + " used block skill");
			} else {
				knockDown(attacker);
			}
			if (ballToScatter != null) { // ball has to scatter after all other actions
				scatterBall(ballToScatter, 1);
				ballToScatter = null;
			}
		} else if (result >= 2) { // push: 2 and 3
			if (result == 4 && !defender.hasSkill("Dodge") || // defender stumbles
					result == 5) { // defender down
				System.out.println("stumble time");
				Runnable knock = new Runnable() {
					@Override
					public void run() {
						if (team1.getPlayersOnPitch().contains(defender)
								|| team2.getPlayersOnPitch().contains(defender)) { // don't do knockdown if already been
																					// pushed off pitch
							knockDown(defender);
						}
						if (ballToScatter != null) {
							System.out.println("scatter time");
							scatterBall(ballToScatter, 1);
						}
						if (taskQueue.size() > 0) {
							taskQueue.pop().run();
						} else {
							sendBlockSuccess(attacker, defender);
						}
					}
				};
				taskQueue.add(knock);
			} else if (result == 4 && defender.hasSkill("Dodge")) {
				sender.sendSkillUsed(game.getId(), defender.getId(), defender.getName(), defender.getTeam(),
						"Dodge In Block");
			}
			pushAction(attacker, defender, followUp);
		}
		// endOfAction(attacker);
		if (attacker.getStatus() != "standing") {
			turnover();
		} else if (result < 2) { // end will be shown within push flow
			sendBlockSuccess(attacker, defender);
		}
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
		if (defender.getStatus() != "standing") {
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

	public void pushAction(PlayerInGame attacker, PlayerInGame defender, boolean followUp) {
		List<Tile> push = calculatePushOptions(attacker, defender);
		if (followUp == true) {
			Tile target = defender.getTile();
			Tile origin = attacker.getTile();
			Runnable follow = new Runnable() {
				@Override
				public void run() {
					attacker.getTile().removePlayer();
					target.addPlayer(attacker);
					System.out.println(attacker.getName() + " follows up to " + target.getLocation()[0] + " "
							+ target.getLocation()[1]);
					sender.sendPushResult(game.getId(), attacker.getId(), attacker.getName(), origin.getLocation(),
							target.getLocation(), "FOLLOW");
					if (!taskQueue.isEmpty()) {
						taskQueue.pop().run();
					} else {
						sendBlockSuccess(attacker, defender);
					}
				}
			};
			if (taskQueue.size() > 0) {
				taskQueue.add(taskQueue.size() - 1, follow); // in case knockdown, needs to be before this
			} else {
				taskQueue.add(follow); // follow up happens after pushes, before scatter or knockdown
			}
		}
		if (push.isEmpty()) {
			pushOffPitch(attacker, defender);
		} else {
			ArrayList<jsonTile> jPush = new ArrayList<>();
			for (Tile t : push) {
				jsonTile jt = new jsonTile(t);
				jt.setTackleZones(null);
				jPush.add(jt);
			}
			Runnable task = new Runnable() {
				@Override
				public void run() {
					carryOutPush(attacker.getId(), defender.getId(), attacker.getLocation(), defender.getLocation(),
							push, followUp);
				}
			};
			taskQueue.addFirst(task);
			int userToChoose = activeTeam.getId();
			if (defender.hasSkill("Side Step") && defender.getTeam() != activeTeam.getId()) {
				userToChoose = defender.getTeam();
				sender.sendSkillUsed(game.getId(), defender.getId(), defender.getName(), defender.getTeam(),
						"Side Step");
			}
			sender.requestPushChoice(game.getId(), attacker.getId(), defender.getId(), attacker.getLocation(),
					defender.getLocation(), jPush, userToChoose);
		}
	}

	public void carryOutPush(int pusher, int pushed, int[] pusherLocation, int[] pushedLocation, List<Tile> options,
			boolean followUp) {
		boolean valid = false;
		for (Tile t : options) {
			if (Arrays.equals(t.getLocation(), runnableLocation[0])) {
				valid = true;
			}
		}
		if (valid == false) {
			throw new IllegalArgumentException("not a valid choice");
		}
		Tile pushChoice = pitch[runnableLocation[0][0]][runnableLocation[0][1]];
		Tile origin = pitch[pushedLocation[0]][pushedLocation[1]];
		PlayerInGame p = getPlayerById(pushed);
		PlayerInGame p2 = getPlayerById(pusher);
		if (pushChoice.containsBall()) {
			Runnable scatter = new Runnable() {
				@Override
				public void run() {
					if (pushChoice.containsBall()) { // in case ball no longer there (i.e. scattered already)
						System.out.println("second scatter?");
						scatterBall(pushChoice, 1);
					}
					if (taskQueue.isEmpty()) {
						sendBlockSuccess(getPlayerById(pusher), getPlayerById(pushed));
					} else {
						taskQueue.pop().run();
					}
				}
			};
			taskQueue.add(scatter); // scatter needs to happen after follow up and knockdown
		}
		if (pushChoice.containsPlayer()) {
			int[] target = runnableLocation[0];
			Runnable task = new Runnable() {
				@Override
				public void run() {
					origin.removePlayer();
					pushChoice.addPlayer(p);
					sender.sendPushResult(game.getId(), pushed, p.getName(), pushedLocation, target, "PUSH");
					if (!taskQueue.isEmpty()) {
						taskQueue.pop().run();
					} else {
						sendBlockSuccess(getPlayerById(pusher), getPlayerById(pushed));
					}
				}
			};
			Runnable task2 = new Runnable() {
				@Override
				public void run() {
					pushAction(getPlayerById(pushed), pushChoice.getPlayer(), false);
				}
			};
			taskQueue.addFirst(task); // final push movement must be first to occur
			taskQueue.addFirst(task2); // must carry out final push action logic first
		} else {
			System.out.println("push result about to send");
			origin.removePlayer();
			pushChoice.addPlayer(p);
			sender.sendPushResult(game.getId(), pushed, p.getName(), pushedLocation, runnableLocation[0], "PUSH");
			// System.out.println(defender.getName() + " is pushed back to " +
			// pushChoice.getLocation()[0] + " "
			// + pushChoice.getLocation()[1]);
		}
		if (!taskQueue.isEmpty()) {
			taskQueue.pop().run();
		} else {
			sendBlockSuccess(getPlayerById(pusher), getPlayerById(pushed));
		}
	}

	public List<Tile> calculatePushOptions(PlayerInGame attacker, PlayerInGame defender) {
		int xOrigin = attacker.getTile().getLocation()[0];
		int yOrigin = attacker.getTile().getLocation()[1];
		List<Tile> options = new ArrayList<>();
		List<Tile> noEmptyOptions = new ArrayList<>(); // for if all push squares have players in
		for (Tile t : defender.getTile().getNeighbours()) {
			int tx = t.getLocation()[0];
			int ty = t.getLocation()[1];
			if (Math.abs(tx - xOrigin) + // corner push
					Math.abs(ty - yOrigin) > 2 || Math.abs(tx - xOrigin) == 2 && // push from above or below
							Math.abs(xOrigin - defender.getTile().getLocation()[0]) == 1
							&& Math.abs(yOrigin - defender.getTile().getLocation()[1]) == 0
					|| Math.abs(ty - yOrigin) == 2 && // push from left or right
							Math.abs(yOrigin - defender.getTile().getLocation()[1]) == 1
							&& Math.abs(xOrigin - defender.getTile().getLocation()[0]) == 0) {
				noEmptyOptions.add(t);
				if (!t.containsPlayer()) {
					options.add(t);
				}
			}
		}
		return options.size() > 0 ? options : noEmptyOptions;
	}

	public void pushOffPitch(PlayerInGame pusher, PlayerInGame pushed) {
		System.out.println(pushed.getName() + " was pushed into the crowd and gets beaten!");
		sender.sendPushResult(game.getId(), pushed.getId(), pushed.getName(), pushed.getLocation(), null, "OFFPITCH");
		Tile origin = pushed.getTile();
		injuryRoll(pushed); // if KO'd or injured it will remove them from pitch
		if (pushed.getStatus() == "stunned") {
			System.out.println(pushed.getName() + " was put back in reserves");
			pushed.setStatus("standing");
			pushed.getTile().removePlayer();
			pushed.getTeamIG().removePlayerFromPitch(pushed);
			pushed.getTeamIG().addToReserves(pushed);
		}
		if (pushed.isHasBall()) {
			System.out.println("pushed off has ball");
			Runnable scatter = new Runnable() {
				@Override
				public void run() {
					scatterBall(origin, 1);
					if (taskQueue.isEmpty()) {
						sendBlockSuccess(pusher, pushed);
					} else {
						taskQueue.pop().run();
					}
				}
			};
			taskQueue.add(scatter); // scatter needs to happen after follow up and knockdown
			if (pushed.getTeamIG() == activeTeam) { // only turnover if player on active team and had ball
				Runnable task2 = new Runnable() {
					@Override
					public void run() {
						turnover();
					}
				};
				taskQueue.add(task2); // scatter needs to happen after follow up and knockdown
			}
		}
		if (taskQueue.size() > 0) {
			taskQueue.pop().run();
		}
	}

	public void touchdown(PlayerInGame p) {
		System.out.println(p.getName() + " scored a touchdown!");
		TeamInGame team = p.getTeamIG();
		TeamInGame tg = null;
		if (team == team1) {
			game.setTeam1Score(game.getTeam1Score() + 1);
			tg = team1;
		} else {
			game.setTeam2Score(game.getTeam2Score() + 1);
			tg = team2;
		}
		sender.sendTouchdown(game.getId(), p.getId(), p.getName(), p.getTeamIG().getId(), p.getTeamIG().getName(),
				game.getTeam1Score(), game.getTeam2Score());
		// if (team1.getTurn() > 8 || team2.getTurn() > 8) {
		// endOfHalf();
		// } else {
		kickOff(tg);
		// }
	}

	public void knockDown(PlayerInGame p) {
		p.setProne();
		Tile location = p.getTile();
		int armour = p.getAV();
		int[] rolls = diceRoller(2, 6);
		int total = rolls[0] + rolls[1];
		sender.sendArmourRoll(game.getId(), p.getId(), p.getName(), armour, rolls,
				total > armour ? "armour was broken" : "armour held");
		if (total > armour) {
			System.out.println(p.getName() + "'s armour was broken.");
			injuryRoll(p);
		} else {
			System.out.println(p.getName() + "'s armour held.");
		}
		if (p.isHasBall() || location.containsBall()) {
			p.setHasBall(false);
			ballToScatter = location;
		}
//		if(p.getTeam() == activeTeam.getId()) {
//		  turnover();
//		}
	}

	public void injuryRoll(PlayerInGame p) {
		int[] rolls = diceRoller(2, 6);
		int total = rolls[0] + rolls[1];
		String outcome = "stunned";
		if (total <= 7) {
			System.out.println(p.getName() + " is stunned");
			p.setStatus("stunned");
		} else {
			// possibility to use apothecary, etc. here
			if (total <= 9) {
				System.out.println(p.getName() + " is KO'd and sent to the dugout");
				outcome = "KO'd and sent to the dugout";
				p.setStatus("KO");
				p.getTeamIG().addToDugout(p);
			} else {
				System.out.println(p.getName() + " is injured and sent to the injury box");
				outcome = "injured and sent to the injury box";
				p.setStatus("injured");
				p.getTeamIG().addToInjured(p);
			}
			p.getTile().removePlayer();
		}
		sender.sendInjuryRoll(game.getId(), p.getId(), p.getName(), rolls, p.getStatus(), p.getLocation(), outcome);
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

	public boolean rerollCheck() {
		return false;
		// placeholder
	}

	// if times > 1 cannot try to catch until final scatter
	public void scatterBall(Tile origin, int times) {
		ballToScatter = null;
		origin.setContainsBall(false);
		int value = diceRoller(1, 8)[0];
		System.out.println("Scatter value: " + value);
		int[] direction = ADJACENT[value - 1];
		System.out.println("Scatter direction: " + direction[0] + direction[1]);
		int[] position = new int[] { origin.getLocation()[0] + direction[0], origin.getLocation()[1] + direction[1] };
		if (position[0] > 0 && position[0] < 26 && position[1] >= 0 && position[1] < 15) {
			Tile target = pitch[position[0]][position[1]];
			System.out.println("Ball scattered to: " + position[0] + " " + position[1]);
			sender.sendBallScatterResult(game.getId(), origin.getLocation(), position);
			if (times > 1) {
				scatterBall(target, times - 1);
				return;
			}
			if (target.containsPlayer()) {
				if (target.getPlayer().isHasTackleZones()) { // will need to make this more specific to catching
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
			if (player.getTeamIG() != activeTeam) {
				turnover();
				return;
			}
		} else {
			System.out.println(player.getName() + " failed to catch the ball!");
			rerollCheck();
			scatterBall(player.getTile(), 1);
			if (!inPassOrHandOff && phase != "kick") {
				Tile scatteredTo = ballLocationCheck();
				if (!scatteredTo.containsPlayer() || scatteredTo.getPlayer().getTeamIG() != activeTeam) {
					turnover(); // only a turnover if ball is not caught by player on active team before comes
								// to
								// rest
				}
			}
		}
	}

	public boolean interceptBallAction(int[] source, PlayerInGame player, boolean reroll) {
		int needed = calculateInterception(player);
		int roll = diceRoller(1, 6)[0];
		System.out.println(player.getName() + " tries to intercept the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " intercepted the ball!");
			player.setHasBall(true);
			sender.sendRollResult(game.getId(), player.getId(), player.getName(), "INTERCEPT", needed, rolled, "success",
					source, player.getLocation(), rerollOptions, player.getTeam(), "Y", reroll);
			turnover();
			return true;
		} else {
			runnableLocation = new int[][] {source, player.getLocation()};
			System.out.println(player.getName() + " failed to intercept the ball!");
			List<String> options = determineRerollOptions("CATCH", player.getId(), new int[][] {source, player.getLocation()});
			String end = "N";
			if(options.isEmpty()) {
				taskQueue.pop().run();
			} else {
				awaitingReroll = new String[] { "Y", "INTERCEPT", "" + player.getId() };
				Runnable task = new Runnable() {
					@Override
					public void run() {
						interceptBallAction(source, player, true);
					}
				};
				taskQueue.addFirst(task);
				end = "N";
			}
			sender.sendRollResult(game.getId(), player.getId(), player.getName(), "INTERCEPT", needed, rolled, "failed",
					source, player.getLocation(), rerollOptions, player.getTeam(), end, reroll);
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
		rollType = "PICKUPBALL";
		rollNeeded = needed;
		rolled.clear();
		rolled.add(roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " picked up the ball!");
			player.setHasBall(true);
			player.getTile().setContainsBall(false);
			rollResult = "success";
			return true;
		} else {
			System.out.println(player.getName() + " failed to pick up the ball!");
			rollResult = "failed";
			return false;
		}
	}

	public void passBallAction(PlayerInGame thrower, Tile target, boolean reroll) {
		int[] details = calculateThrow(thrower, thrower.getTile(), target);
		int needed = details[0];
		int modifier = details[1];
		inPassOrHandOff = true;
		int roll = diceRoller(1, 6)[0];
		rolled.clear();
		rolled.add(roll);
		System.out.println(thrower.getName() + " tries to throw the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll == 1 || roll + modifier <= 1) { // on a natural 1 or 1 after modifiers
			runnableLocation = new int[][] {thrower.getLocation(), target.getLocation()};
			System.out.println(thrower.getName() + " fumbled the ball!");
			List<String> options = determineRerollOptions("THROW", thrower.getId(),
					new int[][] { thrower.getLocation(), target.getLocation() });
			String finalRoll = "N";
			if (options.isEmpty()) {
				finalRoll = "Y";
			} else {
				awaitingReroll = new String[] { "Y", "THROW", "" + thrower.getId() };
				Runnable task = new Runnable() {
					@Override
					public void run() {
						passBallAction(thrower, target, true);
					}
				};
				taskQueue.add(task);
			}
			sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled, "failed",
					thrower.getLocation(), target.getLocation(), options, thrower.getTeam(), finalRoll, reroll);
			if (options.isEmpty()) {
				scatterBall(thrower.getTile(), 1);
				turnover();
			}
		} else {
			if (interceptor != null) {
				Runnable task2 = new Runnable() {
					@Override
					public void run() {
						continuePass(roll, needed, thrower, target);
					}
				};
				taskQueue.add(task2);
				interceptBallAction(thrower.getLocation(), interceptor, false);
			} else {
				continuePass(roll, needed, thrower, target);
			}
		}
	}

	public void continuePass(int roll, int needed, PlayerInGame thrower, Tile target) {
		if (roll >= needed) {
			System.out.println(thrower.getName() + " threw the ball accurately!");
			thrower.setHasBall(false);
			String end = "Y";
			if(target.containsPlayer()) {
				end = "N";
			}
			sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled, "success",
					thrower.getLocation(), target.getLocation(), null, thrower.getTeam(), end, false);
			if (target.containsPlayer()) {
				catchBallAction(target.getPlayer(), true);
			} else {
				target.setContainsBall(true);
			}
		} else {
			System.out.println(thrower.getName() + " threw the ball badly");
			sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled, "badly",
					thrower.getLocation(), target.getLocation(), null, thrower.getTeam(), "N", false);
			scatterBall(target, 3);
			Tile scatteredTo = ballLocationCheck();
			if (!scatteredTo.containsPlayer() || scatteredTo.getPlayer().getTeamIG() != activeTeam) {
				turnover(); // only a turnover if is not caught by player on same team before comes to rest
				return;
			}
		}
		thrower.getTeamIG().setPassed(true);
		inPassOrHandOff = false;
		endOfAction(thrower);
	}

	public void handOffBallAction(PlayerInGame player, Tile target) {
		actionCheck(player);
		if (!player.isHasBall()) {
			throw new IllegalArgumentException("Player doesn't have the ball");
		}
		if (!target.containsPlayer()) {
			throw new IllegalArgumentException("Must hand off ball to a player");
		}
		if (player.getTeamIG().hasHandedOff()) {
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
		int modifier = -2;
		addTackleZones(p);
		modifier += p.getTile().getTackleZones();
		return calculateAgilityRoll(p, p.getTile(), modifier);
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

	public int[] calculateThrow(PlayerInGame thrower, Tile from, Tile target) {
		actionCheck(thrower);
		if (thrower.getTeamIG().hasPassed()) {
			throw new IllegalArgumentException("Can only attempt pass once per turn");
		}
		int[] origin = from.getLocation();
		int[] destination = target.getLocation();
		// rounds down distance to target to nearest square (in a straight line, using
		// Pythagoras' theorem)
		int distance = (int) Math.sqrt(((origin[0] - destination[0]) * (origin[0] - destination[0]))
				+ ((origin[1] - destination[1]) * (origin[1] - destination[1])));
		System.out.println("distance: " + distance);
		int modifier = 0;// short pass
		if (distance > 13) {
			throw new IllegalArgumentException("Cannot throw more than 13 squares");
		} else if (distance < 4) { // quick pass
			modifier = 1;
		} else if (distance > 6 && distance < 11) { // long pass
			modifier = -1;
		} else if (distance > 11) { // bomb
			modifier = -2;
		}
		addTackleZones(thrower);
		modifier += from.getTackleZones();
		return new int[] { calculateAgilityRoll(thrower, from, modifier), modifier };
	}

	public List<PlayerInGame> calculatePossibleInterceptors(List<Tile> path, PlayerInGame thrower) {
		path.remove(path.size() - 1); // if opponent is in target square, they're the target, not an interceptor (can
										// throw to opponent if you want)
		List<PlayerInGame> interceptors = new ArrayList<>();
		for (Tile t : path) {
			if (t.containsPlayer() && t.getPlayer().getTeam() != thrower.getTeam()
					&& t.getPlayer().isHasTackleZones()) {
				interceptors.add(t.getPlayer());
				System.out.println("Possible interception by " + t.getPlayer().getName() + " at " + t.getLocation()[0]
						+ " " + t.getLocation()[1] + " with a roll of " + calculateInterception(t.getPlayer()) + "+");
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
		int x = from.getLocation()[0];
		int y = from.getLocation()[1];
		int xDistance = Math.abs(x - target.getLocation()[0]);
		int yDistance = Math.abs(y - target.getLocation()[1]);
		int n = 1 + xDistance + yDistance;
		int xIncline = (target.getLocation()[0] > x) ? 1 : -1;
		int yIncline = (target.getLocation()[1] > y) ? 1 : -1;
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
		System.out.println("Ball went off pitch from " + origin.getLocation()[0] + " " + origin.getLocation()[1]);
		// determine which side/ orientation
		int[] position = origin.getLocation();
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
			if (target.getPlayer().isHasTackleZones()) {
				catchBallAction(target.getPlayer(), false);
			} else {
				scatterBall(target, 1);
			}
		} else {
			target.setContainsBall(true);
		}
	}

	public void addTackleZones(PlayerInGame player) {
		resetTackleZones();
		List<PlayerInGame> opponents;
		opponents = player.getTeamIG() == team1 ? team2.getPlayersOnPitch() : team1.getPlayersOnPitch();
		for (PlayerInGame p : opponents) {
			if (p.isHasTackleZones()) {
				for (Tile t : p.getTile().getNeighbours()) {
					// System.out.println(t);
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
		List<PlayerInGame> attSupport = getAssists(attacker, defender);
		List<PlayerInGame> defSupport = getAssists(defender, attacker);
		return new int[] { attSupport.size(), defSupport.size() };
	}

	public List<PlayerInGame> getAssists(PlayerInGame p1, PlayerInGame p2) {
		p1.setHasTackleZones(false);
		p2.setHasTackleZones(false);
		addTackleZones(p2);
		List<PlayerInGame> support = new ArrayList<>(p2.getTile().getTacklers());
		List<PlayerInGame> results = new ArrayList<>(); // to prevent concurrent modification errors
		results.addAll(support);
		if (support != null && !support.isEmpty()) {
			for (PlayerInGame p : support) {
				addTackleZones(p);
				Set<PlayerInGame> tacklers = p.getTile().getTacklers(); // set so unique
				ArrayList<PlayerInGame> tacklersA = new ArrayList<>(tacklers); // to prevent concurrent modification
																				// errors
				if (tacklersA != null && !tacklersA.isEmpty()) {
					for (PlayerInGame q : tacklersA) {
						addTackleZones(q);
						if (q.getTile().getTacklers() != null && !q.getTile().getTacklers().isEmpty()) {
							results.remove(p);
						}
					}
				}
			}
		}
		p1.setHasTackleZones(true);
		p2.setHasTackleZones(true);
		return results;
	}

	public void endOfAction(PlayerInGame player) { // will involve informing front end
		System.out.println("end of action");
		player.setActionOver(true);
	}

	public static int[] diceRoller(int quantity, int number) {
		int[] result = new int[quantity];
		Random rand = new Random();
		for (int i = 0; i < quantity; i++) {
			if (testing == true) {
				// manually setting for testing
				result[i] = diceRolls.get(0);
				diceRolls.remove(0);
			} else {
				result[i] = rand.nextInt(number) + 1;
			}
		}
		return result;
	}

	public void actionCheck(PlayerInGame p) {
		if (p.getTeamIG() != activeTeam) {
			throw new IllegalArgumentException("Not that player's turn");
		}
		if (p.getActionOver() == true) {
			System.out.println(p.getName());
			throw new IllegalArgumentException("That player's action has finished for this turn");
		}
		if (p.getStatus() == "stunned") {
			throw new IllegalArgumentException("A stunned player cannot act");
		}
		if (awaitingReroll != null && awaitingReroll[0] == "Y") {
			throw new IllegalArgumentException("We're in a reroll situation");
		}
	}

	public void setActiveTeam(TeamInGame team) {
		activeTeam = team;
	}

	public Tile ballLocationCheck() {
		for (Tile[] array : pitch) {
			for (Tile t : array) {
				if (t.containsBall() || t.containsPlayer() && t.getPlayer().isHasBall()) {
					ballLocation = t.getLocation();
					return t;
				}
			}
		}
		ballLocation = null;
		return null; // should be impossible during main gameplay
	}

	public PlayerInGame getPlayerById(int playerId) {
		PlayerInGame p1 = team1.getPlayerById(playerId);
		if (p1 == null) {
			PlayerInGame p2 = team2.getPlayerById(playerId);
			if (p2 == null) {
				return null;
			} else {
				return p2;
			}
		}
		return p1;
	}

	public void sendTeamsInfo(int teamId) {
		ballLocationCheck();
		int[] ball = ballLocation;
		if (pitch[ballLocation[0]][ballLocation[1]].containsPlayer()) {
			ball = null;
		}
		sender.sendGameStatus(game.getId(), activeTeam.getId(), activeTeam.getName(), team1, team2,
				game.getTeam1Score(), game.getTeam2Score(), ball);
	}

	public void sendRoute(int playerId, int[] from, int[] target, int teamId) {
		List<jsonTile> route = jsonRoute(getOptimisedRoute(playerId, from, target));
		int routeMACost;
		if (route.isEmpty()) {
			routeMACost = 0;
		} else {
			routeMACost = route.size() - 1 + (route.get(1).getStandUpRoll() != null ? 3 : 0);
		}
		sender.sendRoute(game.getId(), teamId, playerId, route, routeMACost);
	}

	public void sendWaypointRoute(int playerId, int[] target, List<int[]> waypoints, int teamId) {
		List<jsonTile> route = jsonRoute(getRouteWithWaypoints(playerId, waypoints, target));
		int routeMACost;
		if (route.isEmpty()) {
			routeMACost = 0;
		} else {
			routeMACost = route.size() - 1 + (route.get(1).getStandUpRoll() != null ? 3 : 0);
		}
		sender.sendRoute(game.getId(), teamId, playerId, route, routeMACost);
	}

	public void carryOutRouteAction(int playerId, List<int[]> route, int teamId) {
		System.out.println("carrying out route");
		routeSaved = false;
		actionsNeeded = 0;
		if (route.isEmpty()) {
			return;
		}
		PlayerInGame p = getPlayerById(playerId);
		List<Tile> tileRoute = new ArrayList<>();
		for (int[] i : route) {
			tileRoute.add(pitch[i[0]][i[1]]);
		}
		List<Tile> moved = movePlayerRouteAction(p, tileRoute);
		List<jsonTile> jsonMoved = new ArrayList<>();
		for (Tile t : moved) {
			jsonTile jt = new jsonTile(t);
			jt.setTackleZones(null);
			jsonMoved.add(jt);
		}
		if (jsonMoved.size() > 1) {
			String end = "N";
			if (jsonMoved.size() == route.size() && taskQueue.size() == 0 && blitz == null) { // if smaller, means a
																								// roll carried out
				end = "Y";
			}
			sender.sendRouteAction(game.getId(), playerId, jsonMoved, end);
			if (taskQueue.size() == 0 && blitz != null) {
				blitz.run();
				return;
			}
		}
		if (p.isHasBall()) {
			System.out.println("checking for touchdown");
			if ((route.get(jsonMoved.size() - 1)[0] == 0 && p.getTeamIG() == team2)
					|| route.get(jsonMoved.size() - 1)[0] == 25 && p.getTeamIG() == team1) {
				touchdown(p);
			}
		}
		if (actionsNeeded > 0) {
			continueAction(playerId, route, jsonMoved, teamId);
		} else {
			if (taskQueue.size() > 0) {
				taskQueue.pop().run();
			} else if (blitz != null) {
				blitz.run();
			}
		}
	}

	public void continueAction(int playerId, List<int[]> route, List<jsonTile> jsonMoved, int teamId) {
		boolean result;
		System.out.println("in continue Action");
		List<int[]> remaining = route.subList(jsonMoved.size(), route.size()); // sublist is exclusive of final
		if (actionsNeeded > 0) {
			actionsNeeded--;
			System.out.println("popping roll");
			System.out.println(actionsNeeded);
			taskQueue.pop().run();

			try {
				System.out.println("waiting for result");
				result = runnableResults.take(); // to wait for runnable
			} catch (Exception e) {
				System.out.println("Thread error, everything breaks");
			}
		}
		if (awaitingReroll != null && awaitingReroll[0] == "Y" && rerollOptions.size() > 0 && routeSaved == false
				&& actionsNeeded == 0) {
			if (remaining.size() > 1) {
				routeSaved = true;
				Runnable task = new Runnable() {
					@Override
					public void run() {
						System.out.println("in carryout route runnable");
						System.out.println("movement left: " + remaining.size());
						awaitingReroll = null;

						carryOutRouteAction(playerId, remaining, teamId);
					}
				};
				taskQueue.add(task);
			}
		}
		String finalRoll = "N";
		if (Arrays.equals(runnableLocation[1], route.get(route.size() - 1))
				&& (awaitingReroll == null || awaitingReroll[0] == "N") && actionsNeeded == 0 && blitz == null) {
			finalRoll = "Y";
		}
		PlayerInGame p = getPlayerById(playerId);
		int[] target = null;
		if (!remaining.isEmpty()) {
			target = route.get(jsonMoved.size());
		}
		sender.sendRollResult(game.getId(), playerId, p.getName(), rollType, rollNeeded, rolled, rollResult,
				route.get(jsonMoved.size() - 1), target, rerollOptions, teamId, finalRoll, false);
		if (rollResult.equals("success")) {
			System.out.println("in roll result success");// no reroll needed so just continue route
			if (actionsNeeded > 0) {
				continueAction(playerId, route, jsonMoved, teamId);
			} else {
				awaitingReroll = null;
				if (finalRoll == "N") {
					carryOutRouteAction(playerId, remaining, teamId);
					return;
				} else if (blitz != null) {
					blitz.run();
				}
			}
		} else if (rerollOptions.isEmpty()) {
			System.out.println("end of the line");
			if (rollType == "DODGE" || rollType == "GFI") {
				System.out.println("please knockdown");
				knockDown(p);
			} else if (rollType == "PICKUPBALL") {
				System.out.println("pick up fail time");
				ballToScatter = pitch[runnableLocation[1][0]][runnableLocation[1][1]];
			}
			if (ballToScatter != null) {
				System.out.println("SCATTER FROM HERE");
				scatterBall(ballToScatter, 1);
				turnover();
			}
			if (p.getStatus() != "standing") {
				turnover();
			}
		} else if (rollResult.equals("failed") && rerollOptions.size() > 0 && actionsNeeded > 0) {
			Runnable task2 = new Runnable() {

				@Override
				public void run() {
					System.out.println("carrying on rolls after reroll");
					continueAction(playerId, route, jsonMoved, teamId);
				}

			};
			taskQueue.add(1, task2);
		}
		if (p.isHasBall()) {
			System.out.println("checking for touchdown");
			if ((route.get(jsonMoved.size() - 1)[0] == 0 && p.getTeamIG() == team2)
					|| route.get(jsonMoved.size() - 1)[0] == 25 && p.getTeamIG() == team1) {
				touchdown(p);
			}
		}
	}

	public List<Tile> movePlayerRouteAction(PlayerInGame p, List<Tile> route) {
		List<Tile> movedSoFar = new ArrayList<>();
		actionCheck(p);
		awaitingReroll = null;
		addTackleZones(p);
		checkRouteValid(p, route);
		p.setActedThisTurn(true);
		if (p.getStatus().equals("prone")) {
			if (!standUpAction(p)) {
				return movedSoFar;
			}
		}
		movedSoFar.add(route.remove(0));
		for (Tile t : route) {
			Tile tempT = p.getTile();
			t.addPlayer(p);
			tempT.setPlayer(null);
			p.decrementRemainingMA();
			int[][] tempLocation = new int[][] { tempT.getLocation(), t.getLocation() };
			if (p.getRemainingMA() < 0) {
				actionsNeeded++;
				Runnable task = new Runnable() {
					@Override
					public void run() {
						System.out.println("in runnable");
						boolean result = goingForItAction(p, tempT, t);
						runnableLocation = tempLocation;
						if (result == false) {
							rerollOptions = determineRerollOptions("GFI", p.getId(), tempLocation);
							if (!rerollOptions.isEmpty()) { // only save task if opportunity for
															// reroll
								awaitingReroll = new String[] { "Y", "GFI", "" + p.getId() };

								Runnable task = new Runnable() {
									@Override
									public void run() {
										System.out.println("in runnable");
										runnableResults.add(goingForItAction(p, tempT, t));
										runnableLocation = tempLocation;
									}
								};
								taskQueue.addFirst(task);
							} else {
								awaitingReroll = new String[] { "N", "GFI", "" + p.getId() };
							}
						} else {
							rerollOptions.clear();
						}
						runnableResults.add(result);
					}
				};
				taskQueue.add(task);
			}
			if (tempT.getTackleZones() != 0) {
				actionsNeeded++;
				Runnable task = new Runnable() {

					@Override
					public void run() {
						System.out.println("In runnable dodge 1");
						System.out.println(taskQueue.size());
						boolean result = dodgeAction(p, tempT, t);
						runnableLocation = tempLocation;
						if (result == false) {
							rerollOptions = determineRerollOptions("DODGE", p.getId(), tempLocation);
							if (!rerollOptions.isEmpty()) { // only save task if opportunity for
															// reroll
								System.out.println("In runnable dodge 2");
								awaitingReroll = new String[] { "Y", "DODGE", "" + p.getId() };

								Runnable task = new Runnable() {

									@Override
									public void run() {
										runnableResults.add(dodgeAction(p, tempT, t));
										runnableLocation = tempLocation;
									}

								};
								taskQueue.addFirst(task);
							} else {
								awaitingReroll = new String[] { "N", "DODGE", "" + p.getId() };
							}
						} else {
							rerollOptions.clear();
						}
						runnableResults.add(result);
					}
				};
				taskQueue.add(task);
			}
			if (t.containsBall()) {
				actionsNeeded++;

				Runnable task = new Runnable() {
					@Override
					public void run() {
						System.out.println("In runnable PICKUPBALL 1");
						boolean result = pickUpBallAction(p);
						runnableLocation = tempLocation;
						if (result == false) {
							rerollOptions = determineRerollOptions("PICKUPBALL", p.getId(), tempLocation);
							if (!rerollOptions.isEmpty()) { // only save task if opportunity for
															// reroll
								awaitingReroll = new String[] { "Y", "PICKUPBALL", "" + p.getId() };

								Runnable task = new Runnable() {

									@Override
									public void run() {
										System.out.println("In runnable PICKUPBALL 2");
										runnableResults.add(pickUpBallAction(p));
										runnableLocation = tempLocation;
									}

								};
								taskQueue.addFirst(task);
							}
						} else {
							rerollOptions.clear();
						}
						runnableResults.add(result);
					}
				};
				taskQueue.add(task);
			}

			if (actionsNeeded > 0) {
				return movedSoFar;
			}
			System.out.println(p.getName() + " moved to: " + t.getLocation()[0] + " " + t.getLocation()[1]);
			movedSoFar.add(t);
		}
		return movedSoFar;
	}

	public List<String> determineRerollOptions(String action, int playerId, int[][] location) {
		List<String> results = new ArrayList<>();
		if (action != "BLOCK" && awaitingReroll != null && location[0].equals(runnableLocation[0])
				&& location[1].equals(runnableLocation[1]) && playerId == Integer.parseInt(awaitingReroll[2])
				&& action == awaitingReroll[1]) { // means in a
													// reroll -
													// can't reroll
													// something
													// more than
													// once
			return results;
		}
		PlayerInGame p = getPlayerById(playerId);
		if (p.getTeamIG() == activeTeam && !activeTeam.hasRerolled() && activeTeam.getRemainingTeamRerolls() > 0) {
			results.add("Team Reroll");
		}
		if (action == "DODGE") {
			if (p.hasSkill("Dodge") && !p.hasUsedSkill("Dodge")) {
				results.add("Dodge Skill");
			}
		} else if (action == "PICKUPBALL") {
			if (p.hasSkill("Sure Hands")) {
				results.add("Sure Hands Skill");
			}
		} else if (action == "THROW") {
			if (p.hasSkill("Pass")) {
				results.add("Pass Skill");
			}
		} else if (action == "CATCH") {
			if(p.hasSkill("Catch")) {
				results.add("Catch Skill");
			}
		}
		return results;
	}

	public void carryOutReroll(int playerId, int team, String rerollChoice) {
		sender.sendRerollChoice(game.getId(), playerId, team,
				(team == team1.getId() ? team1.getName() : team2.getName()), rerollChoice, runnableLocation);
		System.out.println("in reroll");
		PlayerInGame p = getPlayerById(playerId);
		if (awaitingReroll == null || Integer.parseInt(awaitingReroll[2]) != playerId) {
			throw new IllegalArgumentException("Invalid details");
		}
		if (!rerollChoice.contains("Don't reroll")) {
			System.out.println(rerollChoice);
			if (rerollChoice.equals("Team Reroll")) {
				System.out.println("In Team Reroll");
				activeTeam.useTeamReroll();
			} else if (rerollChoice.contains("Skill")) {
				p.useSkill(rerollChoice.replace(" Skill", ""));
				System.out.println("In skill reroll");
			}
			awaitingReroll = null;
			taskQueue.pop().run();
			if(rollType == "THROW" || rollType == "INTERCEPT") {
				return;
			}
			if (rollType == "BLOCK") {
				taskQueue.pop(); // kill saved task for not rerolling
				return;
			}
			boolean result = false;
			try {
				System.out.println("waiting for result");
				result = runnableResults.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String end = "N";
			if (result == true && taskQueue.isEmpty() && blitz == null) {
				end = "Y";
			}
			sender.sendRollResult(game.getId(), playerId, p.getName(), rollType, rollNeeded, rolled, rollResult,
					runnableLocation[0], runnableLocation[1], new ArrayList<String>(), activeTeam.getId(), end, true);
			if (result == true) {
				if (!taskQueue.isEmpty()) {
					System.out.println("continuing route");
					taskQueue.pop().run();
				} else if (blitz != null) {
					blitz.run();
				}
				return;
			}
		}
		if (rollType == "DODGE" || rollType == "GFI") {
			knockDown(p);
		} else if (rollType == "PICKUPBALL") {
			ballToScatter = pitch[runnableLocation[1][0]][runnableLocation[1][1]];
		} else if (rollType == "BLOCK") {
			// don't reroll, but continue by asking for dice choice
			taskQueue.pop();
			taskQueue.pop().run();
			return;
		}
		if (ballToScatter != null) {
			scatterBall(ballToScatter, 1);
			turnover();
		}
		if (p.getStatus() != "standing") {
			ballToScatter = null;
			turnover();
		}
	}

	public void sendBlockDetails(int player, int opponent, int[] location, int team) {
		PlayerInGame attacker = getPlayerById(player);
		actionCheck(attacker);
		if (attacker.isActedThisTurn()) {
			throw new IllegalArgumentException("Can't block after acting, except when blitzing");
		}
		PlayerInGame defender = getPlayerById(opponent);
		int[] block = calculateBlock(getPlayerById(player), pitch[location[0]][location[1]], getPlayerById(opponent));
		int[][] attLocations = getJsonFriendlyAssists(attacker, defender);
		int[][] defLocations = getJsonFriendlyAssists(defender, attacker);
		sender.sendBlockInfo(game.getId(), player, opponent, location, defender.getLocation(), block, attLocations,
				defLocations, team);
	}

	public int[][] getJsonFriendlyAssists(PlayerInGame attacker, PlayerInGame defender) {
		List<PlayerInGame> support = getAssists(attacker, defender);
		// just send assist locations to save amount of data sent
		int[][] assistLocations = new int[support.size()][2];
		for (int i = 0; i < support.size(); i++) {
			assistLocations[i] = support.get(i).getLocation();
		}
		return assistLocations;
	}

	public void carryOutBlock(int player, int opponent, int[] location, boolean followUp, boolean reroll, int team) {
		PlayerInGame attacker = getPlayerById(player);
		PlayerInGame defender = getPlayerById(opponent);
		int[] details = blockAction(attacker, defender, followUp);
		int[][] attLocations = getJsonFriendlyAssists(attacker, defender);
		int[][] defLocations = getJsonFriendlyAssists(defender, attacker);
		runnableLocation = new int[][] { location, defender.getLocation() };
		if (rerollOptions.size() > 0) {
			Runnable task = new Runnable() {
				@Override
				public void run() {
					System.out.println("in block reroll");
					awaitingReroll = null;
					carryOutBlock(player, opponent, location, followUp, true, team);
				}
			};
			taskQueue.add(task);
			// for if they choose not to reroll
			Runnable task2 = new Runnable() {
				@Override
				public void run() {
					System.out.println("in block not rerolling");
					awaitingReroll = null;
					sender.requestBlockDiceChoice(game.getId(), player, opponent, details[1]);
				}
			};
			taskQueue.add(task2);
		}
		sender.sendBlockDiceResult(game.getId(), player, attacker.getName(), opponent, defender.getName(), location,
				defender.getLocation(), rolled, attLocations, defLocations, rerollOptions, reroll, team);
		if (rerollOptions == null || rerollOptions.size() == 0) {
			sender.requestBlockDiceChoice(game.getId(), player, opponent, details[1]);
		}
	}

	public void carryOutBlockChoice(int diceChoice, int player, int opponent, boolean followUp, int team) {
		if (blitz == null) {
			getPlayerById(player).setActionOver(true);
		}
		getPlayerById(player).setActedThisTurn(true);
		sender.sendBlockDiceChoice(game.getId(), player, opponent, rolled.get(diceChoice),
				team == team1.getId() ? team1.getName() : team2.getName(), team);
		System.out.println("dice choice: " + diceChoice);
		// System.out.println("rolled: " + rolled.get(0));
		// System.out.println("rolled: " + rolled.get(1));
		System.out.println("chosen: " + rolled.get(diceChoice));
		blockChoiceAction(rolled.get(diceChoice) - 1, getPlayerById(player), getPlayerById(opponent), followUp); // need
																													// to
																													// sort
																													// out
																													// follow
																													// up
		// blockChoiceAction(1, getPlayerById(player), getPlayerById(opponent),
		// followUp); // need to sort out follow up

	}

	public void carryOutPushChoice(int[] choice) {
		runnableLocation = new int[][] { choice };
		taskQueue.pop().run();
	}

	public void carryOutBlitz(Integer player, Integer opponent, List<int[]> route, int[] target, boolean followUp,
			int team) {
		blitzAction(getPlayerById(player), getPlayerById(opponent), route, followUp);
	}

	public void sendBlockSuccess(PlayerInGame attacker, PlayerInGame defender) {
		if (blitz != null && activePlayer.getStatus() == "standing") {
			// activePlayer.setActionOver(false);
		}
		sender.sendBlockSuccess(game.getId(), attacker.getId(), defender.getId(), blitz != null);
		blitz = null;
		taskQueue.clear();
	}

	public ArrayList<String> getPossibleActions(PlayerInGame player) {
		if (player.getTeam() != activeTeam.getId()) {
			throw new IllegalArgumentException("Not their turn");
		}
		ArrayList<String> actions = new ArrayList<>();
		if (player.getActionOver() == true || player.getStatus() == "stunned") {
			actions.add("None");
			return actions;
		}
		if (player.getStatus() == "prone") {
			actions.add("standUp");
		}
		if (!activeTeam.hasBlitzed() && player.getActedThisTurn() == false) {
			actions.add("blitz");
		}
		if (player.getRemainingMA() > -3) {
			actions.add("move");
		}
		if (player.isHasBall()) {
			if (!activeTeam.hasPassed()) {
				actions.add("throw");
			}
			if (!activeTeam.hasHandedOff()) {
				actions.add("handOff");
			}
		}
		addTackleZones(player);
		if (player.getTile().getTackleZones() != 0 && player.getActedThisTurn() == false) {
			actions.add("block");
		}
		return actions;
	}

	public void showPossibleActions(Integer player, int team) {
		List<String> actions = getPossibleActions(getPlayerById(player));
		sender.sendPossibleActions(game.getId(), player, getPlayerById(player).getLocation(), actions, team);
	}

	public void sendThrowDetails(Integer player, int[] target, int team) {
		PlayerInGame p = getPlayerById(player);
		Tile goal = pitch[target[0]][target[1]];
		int roll = calculateThrow(p, p.getTile(), goal)[0];
		List<PlayerInGame> interceptors = calculatePossibleInterceptors(calculateThrowTiles(p, p.getTile(), goal), p);
		List<jsonTile> interceptLocations = new ArrayList<>();
		for (PlayerInGame pg : interceptors) {
			jsonTile jt = new jsonTile();
			jt.setPosition(pg.getLocation());
			jt.setCatchRoll(calculateInterception(pg));
			jt.setDescription(pg.getName());
			interceptLocations.add(jt);
		}
		int catchRoll = 0;
		String targetName = null;
		if (goal.containsPlayer()) {
			catchRoll = calculateCatch(goal.getPlayer(), true);
			targetName = goal.getPlayer().getName();
		}
		sender.sendThrowDetails(game.getId(), player, p.getLocation(), target, targetName, roll, catchRoll,
				interceptLocations, team);
	}

	public List<jsonTile> calculateThrowRanges(Integer player, int[] location) {
		List<jsonTile> results = new ArrayList<>();
		for (Tile[] array : pitch) {
			for (Tile t : array) {
				int[] place = t.getLocation();
				jsonTile jt = new jsonTile();
				int distance = (int) Math.sqrt(((place[0] - location[0]) * (place[0] - location[0]))
						+ ((place[1] - location[1]) * (place[1] - location[1])));
				if (distance < 14) {
					jt.setPosition(place);
					if (distance < 4) {
						jt.setDescription("quick pass");

					} else if (distance < 7) {
						jt.setDescription("short pass");
					} else if (distance < 11) {
						jt.setDescription("long pass");
					} else {
						jt.setDescription("long bomb");
					}
					results.add(jt);
				}
			}
		}
		return results;
	}

	public void sendThrowRange(Integer player, int[] location, int team) {
		List<jsonTile> squares = calculateThrowRanges(player, location);
		sender.sendThrowRanges(game.getId(), player, location, squares, team);
	}

	public void carryOutThrow(Integer player, int[] location, int[] target, int team) {
		interceptor = null;
		PlayerInGame thrower = getPlayerById(player);
		actionCheck(thrower);
		if (!thrower.isHasBall()) {
			throw new IllegalArgumentException("Player doesn't have the ball");
		}
		if (thrower.getTeamIG().hasPassed()) {
			throw new IllegalArgumentException("Can only attempt pass once per turn");
		}
		Tile goal = pitch[target[0]][target[1]];
		List<Tile> path = calculateThrowTiles(thrower, thrower.getTile(), goal);
		List<PlayerInGame> interceptors = calculatePossibleInterceptors(path, thrower);
		if (interceptors.size() > 1) {
			Runnable task = new Runnable() {
				@Override
				public void run() {
					passBallAction(thrower, goal, false);
				}
			};
			taskQueue.add(task);
		//	requestInterceptor(interceptors);
			return;
		} else if (interceptors.size() == 1) {
			interceptor = interceptors.get(0);
		}

		passBallAction(getPlayerById(player), goal, false);

	}
}