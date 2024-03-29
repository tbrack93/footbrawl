package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

import com.project.footbrawl.DAO.GameRepository;
import com.project.footbrawl.entity.Game;
import com.project.footbrawl.instance.PlayerInGame;
import com.project.footbrawl.instance.TeamInGame;
import com.project.footbrawl.instance.Tile;
import com.project.footbrawl.instance.jsonTile;

// controls a game's logic and progress
// future: contain DTO for database interactions

@Service
@Scope("prototype")
public class GamePlayService {

	@Autowired
	MessageEncoderService sender;

	@Autowired
	GameRepository gameRepo;

	private static List<Integer> diceRolls = new ArrayList<>(
			Arrays.asList(new Integer[] { 1, 1, 1, 1, 1, 6, 6, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6, 6, 6, 6, 6 }));
	private static boolean testing = false;

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
	private PlayerInGame activePlayer;
	private TeamInGame team1;
	private TeamInGame team2;
	private Tile[][] pitch;
	private boolean team1Assigned;
	private boolean team2Assigned;
	private boolean team1Joined;
	private boolean team2Joined;
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
	private boolean turnedOver;
	private Runnable blitz;
	private boolean isFollowUp;
	private PlayerInGame interceptor;
	private List<PlayerInGame> interceptors;
	private Date created;

	public GamePlayService() {
		team1Assigned = false;
		team2Assigned = false;
		waitingForPlayers = true;
		created = new Date();
	}

	public Date getCreated() {
		return created;
	}

	public boolean isGameFinished() {
		return phase == "ended";
	}

	public int assignPlayer(int teamId) {
		System.out.println(teamId);
		if (teamId == 0) {
			if (team1Assigned == false) {
				team1Assigned = true;
				if (team2Assigned == true) {
					waitingForPlayers = false;
				}
				return 1;
			} else if (team2Assigned == false) {
				team2Assigned = true;
				if (team1Assigned == true) {
					waitingForPlayers = false;
				}
				return 2;
			}
		} else if (teamId == 1) {
			if (team1Assigned == true) {
				throw new IllegalArgumentException("already taken");
			}
			team1Assigned = true;
			if (team2Assigned == true) {
				waitingForPlayers = false;
			}
			return 1;
		} else {
			if (team2Assigned == true) {
				throw new IllegalArgumentException("already taken");
			}
			team2Assigned = true;
			if (team1Assigned == true) {
				waitingForPlayers = false;
			}
			return 2;
		}
		return -1;
	}

	public boolean isTeam1Assigned() {
		return team1Assigned;
	}

	public boolean isTeam2Assigned() {
		return team2Assigned;
	}

	public boolean isWaitingForPlayers() {
		return waitingForPlayers;
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
		inPassOrHandOff = false;
		rerollOptions = new ArrayList<>();
		inTurnover = false;
		pitch = new Tile[26][15];
		for (int row = 0; row < 26; row++) {
			for (int column = 0; column < 15; column++) {
				pitch[row][column] = new Tile(row, column);
			}
		}
		setTileNeighbours(); // doing it once and saving in Tile objects saves repeated computations
		waitingForPlayers = true;
		phase = "pre-game";
	}

	public int getGameId() {
		return game.getId();
	}

	public void joinGame(int team) {
		sender.sendTeamsInfo(game.getId(), team, team1, team2, phase);
		if (phase == "pre-game") {
			if (team == team1.getId()) {
				team1Joined = true;
			} else if (team == team2.getId()) {
				team2Joined = true;
			}
			if (team1Joined == true && team2Joined == true) {
				startGame();
			} else {
				sender.sendWaitingForOpponent(game.getId(), team);
			}
		} else if (phase == "main game") {
			sender.sendGameStatus(game.getId(), activeTeam.getId(), activeTeam.getName(), team1, team2,
					game.getTeam1Score(), game.getTeam2Score(), ballLocationCheck().getLocation(), phase);
		}
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
		game.setStatus("started");
		gameRepo.save(game);
		coinToss();
	}

	public void coinToss() {
		if (diceRoller(1, 2)[0] == 1) {
			activeTeam = team1;
		} else {
			activeTeam = team2;
		}
		System.out.println(activeTeam.getName() + " won the coin toss!");
		sender.sendCoinTossWinner(game.getId(), activeTeam.getId(), activeTeam.getName());
	}

	public void chooseKickOff(int team, String choice) {
		if (team != activeTeam.getId()) {
			throw new IllegalArgumentException("Not their choice");
		}
		sender.sendKickOffChoice(game.getId(), team, activeTeam.getName(), choice);
		if (choice.contains("Kick")) {
			lastKickOff = activeTeam;
		} else {
			lastKickOff = activeTeam.getId() == team1.getId() ? team2 : team1;
		}
		activeTeam = lastKickOff;
		kickOff(activeTeam);
	}

	public void kickOff(TeamInGame kicking) {
		kickingSetupDone = false;
		receivingSetupDone = false;
		if (half != 0) { // no KO's if first kickoff
			checkKOs();
		}
		team1.newKickOff();
		team2.newKickOff();
		Tile ball = ballLocationCheck();
		if (ball != null) {
			if (ball.containsPlayer()) {
				ball.getPlayer().setHasBall(false);
			} else {
				ball.removeBall();
			}
		}
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
		sender.sendGameStatus(game.getId(), activeTeam.getId(), activeTeam.getName(), team1, team2,
				game.getTeam1Score(), game.getTeam2Score(), null, phase);
		getTeamSetup(kicking);
		// team setup, starts with kicking team
		// choose target to kick to. Must be in opponent's half
		// roll on kick off table (low priority)
		// kick takes place, ball scatters
		// receiving team's turn
	}

	public void getTeamSetup(TeamInGame team) {
		sender.sendSetupRequest(game.getId(), team.getName(), team.getId());
	}

	public void playerPlacement(PlayerInGame player, int[] position) {
		if (player.getTeamIG() != activeTeam) {
			throw new IllegalArgumentException("Not your setup phase/ player");
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
			} else if (player.getTile() != null) {
				Tile tempT = player.getTile();
				tempT.removePlayer();
				target.addPlayer(player);
			} else {
				target.addPlayer(player);
			}
			System.out.println(player.getName() + " placed at " + position[0] + " " + position[1]);
			player.getTeamIG().addPlayerOnPitch(player);
			sender.sendSetupUpdate(game.getId(), activeTeam, activeTeam == team1 ? 1 : 2);
		}
	}

	public void removePlayerFromPitch(PlayerInGame player) {
		if (player.getTeamIG() != activeTeam) {
			throw new IllegalArgumentException("Not your setup phase/ player");
		}
		player.getTile().removePlayer();
		player.getTeamIG().addToReserves(player);
		sender.sendSetupUpdate(game.getId(), activeTeam, activeTeam == team1 ? 1 : 2);
	}

	public void endTeamSetup(TeamInGame team) {
		if (checkTeamSetupValid(team)) {
			activeTeam = activeTeam == team1 ? team2 : team1;
			if (kickingSetupDone == false) {
				kickingSetupDone = true;
				getTeamSetup(activeTeam);
			} else {
				receivingSetupDone = true;
				getKickTarget(activeTeam);
			}
		}
	}

	public void getKickTarget(TeamInGame kicking) {
		phase = "kick";
		System.out.println("requesting kick request");
		sender.sendKickRequest(game.getId(), kicking.getId(), kicking.getName());
	}

	public boolean checkTeamSetupValid(TeamInGame team) {
		if (team.getPlayersOnPitch().size() < 11 && !team.getReserves().isEmpty()) {
			sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
					"Must place 11 players on pitch, or as many as you can");
			throw new IllegalArgumentException("Must place 11 players on pitch, or as many as you can");
		}
		int wideZone1 = 0;
		int wideZone2 = 0;
		int scrimmage = 0;
		for (PlayerInGame p : team.getPlayersOnPitch()) {
			if (p.getTile().getLocation()[1] >= 0 && p.getTile().getLocation()[1] <= 3) {
				wideZone1++;
			} else if (p.getTile().getLocation()[1] >= 11 && p.getTile().getLocation()[1] <= 14) {
				wideZone2++;
			} else if (team == team1 && p.getTile().getLocation()[0] == 12
					|| team == team2 && p.getTile().getLocation()[0] == 13) {
				scrimmage++;
			}
		}
		if (wideZone1 > 2 || wideZone2 > 2) {
			sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
					"Cannot have more than 2 players in a widezone");
			throw new IllegalArgumentException("Cannot have more than 2 players in a widezone");
		}
		if (scrimmage < 3 && team.getPlayersOnPitch().size() + team.getReserves().size() >= 3) {
			sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
					"Must have at least 3 players on line of scrimmage, or as many as you can");
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
		if (team == team1 && target.getLocation()[0] > 12 || team == team2 && target.getLocation()[0] < 13) {
			sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
					"Must be placed in your half of the pitch");
			throw new IllegalArgumentException("Must be placed in your half of the pitch");
		}
		if (!target.containsPlayer()) {
			if (team.getPlayersOnPitch().size() >= 11 && !player.getTeamIG().getPlayersOnPitch().contains(player)) {
				sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
						"Cannot have more than 11 players on the pitch");
				throw new IllegalArgumentException("Cannot have more than 11 players on the pitch");
			}
			if (target.getLocation()[1] >= 0 && target.getLocation()[1] <= 3) {
				int wideZone1 = 0;
				for (PlayerInGame p : team.getPlayersOnPitch()) {
					if (p.getTile().getLocation()[1] >= 0 && p.getTile().getLocation()[1] <= 3 && p != player) {
						wideZone1++;
					}
				}
				if (wideZone1 >= 2) {
					sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
							"Cannot have more than 2 players in a widezone");
					throw new IllegalArgumentException("Cannot have more than 2 players in a widezone");
				}
			} else if (target.getLocation()[1] >= 11 && target.getLocation()[1] <= 14) {
				int wideZone2 = 0;
				for (PlayerInGame p : team.getPlayersOnPitch()) {
					if (p.getTile().getLocation()[1] >= 11 && p.getTile().getLocation()[1] <= 14 && p != player) {
						wideZone2++;
					}
				}
				if (wideZone2 >= 2) {
					sender.sendInvalidMessage(game.getId(), team.getId(), "PLACEMENT",
							"Cannot have more than 2 players in a widezone");
					throw new IllegalArgumentException("Cannot have more than 2 players in a widezone");
				}
			}
		}
		return true; // if contains a player (on right side), must be valid for other checks
	}

	public void checkKOs() {
		System.out.println("Checking if KO'd players wake up");
		if (team1.getDugout().isEmpty()) {
			System.out.println("Team 1 has no KO'd players");
		} else {
			for (PlayerInGame p : new ArrayList<PlayerInGame>(team1.getDugout())) {
				String outcome;
				rolled.clear();
				int result = diceRoller(1, 6)[0];
				rolled.add(result);
				if (result >= 4) {
					p.setStatus("standing");
					team1.addToReserves(p);
					team1.removeFromDugout(p);
					System.out.println(p.getName() + " wakes up and joins the rest of the team");
					outcome = " wakes up and joins the team's reserves";
				} else {
					System.out.println(p.getName() + " is still KO'd");
					outcome = "is still KO'd";
				}
				sender.sendKOResult(game.getId(), team1.getName(), p.getId(), p.getName(), rolled, 4, outcome);
			}
		}
		if (team2.getDugout().isEmpty()) {
			System.out.println("Team 2 has no KO'd players");
		} else {
			for (PlayerInGame p : new ArrayList<PlayerInGame>(team2.getDugout())) {
				rolled.clear();
				int result = diceRoller(1, 6)[0];
				rolled.add(result);
				String outcome;
				if (result >= 4) {
					p.setStatus("standing");
					team2.addToReserves(p);
					team2.removeFromDugout(p);
					System.out.println(p.getName() + " wakes up and joins the rest of his team");
					outcome = " wakes up and joins the team's reserves";
				} else {
					System.out.println(p.getName() + " is still KO'd");
					outcome = " is still KO'd";
				}
				sender.sendKOResult(game.getId(), team2.getName(), p.getId(), p.getName(), rolled, 4, outcome);
			}
		}
	}

	public void kickBall(int[] target) {
		phase = "kick";
		Tile goal = pitch[target[0]][target[1]];
		if (activeTeam == team2 && goal.getLocation()[0] > 12 || activeTeam == team1 && goal.getLocation()[0] < 13) {
			sender.sendInvalidMessage(game.getId(), activeTeam.getId(), "KICK",
					"Must kick to opponent's half of the pitch");
			throw new IllegalArgumentException("Must kick to opponent's half of the pitch");
		}
		PlayerInGame kicker = getKicker(); // use furthest back player
		int value = diceRoller(1, 8)[0];
		int[] direction = ADJACENT[value - 1];
		int distance = diceRoller(1, 6)[0];
		int[] position = new int[] { target[0] + direction[0] * distance, target[1] + direction[1] * distance };
		sender.sendKickTarget(game.getId(), kicker.getId(), kicker.getName(), kicker.getLocation(), position);
		// sender.sendBallScatterResult(game.getId(), target, position);
		if (position[0] >= 0 && position[0] < 26 && position[1] >= 0 && position[1] < 15) {
			goal = pitch[position[0]][position[1]];
			goal.setContainsBall(true);
			System.out.println("Ball flew to: " + position[0] + " " + position[1]);
			if (activeTeam == team2 && goal.getLocation()[0] > 12
					|| activeTeam == team1 && goal.getLocation()[0] < 13) {
				System.out.println("Ball landed on kicking team's side, so receivers are given the ball");
				getTouchBack(activeTeam == team1 ? team2 : team1);
				return;
			}
			if (goal.containsPlayer()) {
				if (goal.getPlayer().isHasTackleZones()) { // will need to make this more specific to catching
					System.out.println("active in kick:" + activeTeam.getId());
					catchBallAction(goal.getPlayer(), false);
					return;
				} else {
					scatterBall(goal, 1); // if player can't catch, will scatter again
					return;
				}
			}
		} else {
			getTouchBack(activeTeam == team1 ? team2 : team1);
			return;
		}
		scatterBall(goal, 1);
	}

	public void checkForTouchBack() {
		System.out.println("in touch back check");
		Tile scatteredTo = ballLocationCheck();
		if (scatteredTo == null || activeTeam == team2 && scatteredTo.getLocation()[0] > 12
				|| activeTeam == team1 && scatteredTo.getLocation()[0] < 13) {
			System.out.println("Ball ended on kicking team's side, so receivers are given the ball");
			getTouchBack(activeTeam == team1 ? team2 : team1);
		} else {
			phase = "main game";
			activeTeam = (activeTeam == team1 ? team2 : team1);
			activeTeam.incrementTurn();
			newTurn();
		}
	}

	public void actOnTouchBack(int playerId, int teamId) {
		PlayerInGame p = getPlayerById(playerId);
		if (p.getTeam() != teamId) {
			throw new IllegalArgumentException("Not yours to choose");
		}
		Tile location = ballLocationCheck();
		if (location != null) {
			if (location.containsPlayer()) {
				location.getPlayer().setHasBall(false);
			} else {
				location.removeBall();
			}
		}
		p.setHasBall(true);
		sender.sendTouchBackResult(game.getId(), playerId, p.getName());
		phase = "main game";
		activeTeam = (activeTeam == team1 ? team2 : team1);
		activeTeam.incrementTurn();
		newTurn();
	}

	public PlayerInGame getKicker() {
		List<PlayerInGame> possible = new ArrayList<>(activeTeam.getPlayersOnPitch());
		int furthestBack = 12;
		PlayerInGame best = possible.get(0);
		for (PlayerInGame p : possible) {
			int[] placement = p.getLocation();
			if (placement[1] >= 4 && placement[1] <= 10) { // not in widezone
				if (activeTeam.getId() == team1.getId() && placement[0] < furthestBack) {
					furthestBack = placement[0];
					best = p;
				} else if (activeTeam.getId() == team2.getId() && placement[0] > furthestBack) {
					furthestBack = placement[0];
					best = p;
				}
			}
		}
		return best;
	}

	public void getTouchBack(TeamInGame team) {
		System.out.println("active" + activeTeam.getId());
		List<jsonTile> options = new ArrayList<>();
		List<PlayerInGame> possibles = new ArrayList<>(team.getPlayersOnPitch());
		for (PlayerInGame p : possibles) {
			jsonTile jt = new jsonTile();
			jt.setPosition(p.getLocation());
			options.add(jt);
		}
		String description = "Ball landed in wrong half";
		Tile locate = ballLocationCheck();
		if (locate == null) {
			description = "Ball went off pitch";
		} else {
			locate.removeBall();
		}
		sender.sendTouchBackRequest(game.getId(), options, team.getId(), description);
	}

	public void endOfHalf() {
		half++;
		if (half == 2) {
			newHalf();
		} else if (half == 3) {
//			if (game.getTeam1Score() == game.getTeam2Score()) {
//				extraTime();
//			} else {
//				endGame(game.getTeam1Score() > game.getTeam2Score() ? team1 : team2);
//			}
//		} else if (half == 4) {
//			if (game.getTeam1Score() == game.getTeam2Score()) {
//				penaltyShootOuts();
//			} else {
//				endGame(game.getTeam1Score() > game.getTeam2Score() ? team1 : team2);
//			}
			TeamInGame winners;
			if (game.getTeam1Score() == game.getTeam2Score()) {
				winners = null;
			} else if (game.getTeam1Score() > game.getTeam2Score()) {
				winners = team1;
			} else {
				winners = team2;
			}
			endGame(winners);
		}
		// check if end of game & if winner
		// if not start new half or extra time
		// if extra time and no winner, go to penalty shoot out
	}

	public void endGame(TeamInGame winners) {
		if (winners == null) {
			System.out.println("It's a draw!");
			sender.sendGameEnd(game.getId(), "Draw", 0, game.getTeam1Score(), game.getTeam2Score());
		} else {
			System.out.println(winners.getName() + " won the match!");
			sender.sendGameEnd(game.getId(), winners.getName(), winners.getId(), game.getTeam1Score(),
					game.getTeam2Score());
		}
		phase = "ended";
		game.setStatus("ended");
		gameRepo.save(game);
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
		String details = "";
		if (half == 1) {
			details = "First half begins!";
		} else if (half == 2) {
			details = "Second half begins!";
		} else if (half == 3) {
			details = "Extra time begins!";
		}
		sender.sendNewHalf(game.getId(), details);
		// new kickoff with team that received start of last half
		kickOff(lastKickOff == team1 ? team2 : team1);
		// some inducements may come into play here
	}

	public void turnover() {
		System.out.println("checking for turnover");
		if (turnedOver == false) {
			turnedOver = true;
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
		team1.endTurn();
		team2.endTurn();
		for (PlayerInGame p : team1.getAwoken()) {
			p.wakeUp();
			sender.sendPlayerWokeUp(game.getId(), p.getId(), p.getName());
		}
		team1.resetAwoken();
		for (PlayerInGame p : team2.getAwoken()) {
			p.wakeUp();
			sender.sendPlayerWokeUp(game.getId(), p.getId(), p.getName());
		}
		team2.resetAwoken();
		blitz = null;
		activeTeam = (activeTeam == team1 ? team2 : team1);
		if (activeTeam.getTurn() == 8 && half == 1 || activeTeam.getTurn() == 16 && half == 2) {
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
		System.out.println("new turn");
		activeTeam.newTurn();// reset players on pitch (able to move/ act)
		sender.sendGameStatus(game.getId(), activeTeam.getId(), activeTeam.getName(), team1, team2,
				game.getTeam1Score(), game.getTeam2Score(), ballLocationCheck().getLocation(), phase);
		sender.sendNewTurn(game.getId(), activeTeam.getId(), activeTeam.getName());
		taskQueue.clear();
	}

	public void showPossibleMovement(int playerId, int[] location, int maUsed, int requester) {
		if (phase != "main game") {
			return;
		}
		inTurnover = false;
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
					System.out.println("updating activePlayer");
					makeActivePlayer(p);
				}
				resetTiles();
				p.setRemainingMA(originalMA - maUsed);
				Tile position = pitch[location[0]][location[1]];
				int cost = 0;
				if (p.getStatus().equals("prone")) {
					p.setRemainingMA(p.getRemainingMA() - 3);
				}
				addTackleZones(p);
				searchNeighbours(p, position, cost);

				for (int i = 0; i < 26; i++) {
					for (int j = 0; j < 15; j++) {
						Tile t = pitch[i][j];
						if (t.getCostToReach() != 99) {
							jsonTile jTile = new jsonTile(t);
							if (t.getGoForIt() == true && t != position) {
								jTile.setGoingForItRoll(2); // if blizzard this will be 3
							}
							squares.add(jTile);
						}
					}
				}
			}
		}
		p.setRemainingMA(originalMA);
//		for(Tile[] array: pitch) {
//			for(Tile t : array) {
//				System.out.println(t.getCostToReach());
//		    }
//		}
		if (squares.size() == 1) {
			squares.clear();
		}

		sender.sendMovementInfoMessage(game.getId(), requester, playerId, squares);

	}

	// Breadth first to determine where can move
	public void searchNeighbours(PlayerInGame p, Tile location, int cost) {
		LinkedList<Tile> queue = new LinkedList<>();
		location.setCostToReach(cost);
		queue.add(location);
		// System.out.println(p.getRemainingMA());
		while (!queue.isEmpty()) {
			Tile temp = queue.poll();
			cost = temp.getCostToReach();
			if (temp.getCostToReach() < p.getRemainingMA() + 2) {
				for (Tile t : temp.getNeighbours()) {
					if (!t.containsPlayer() || t.containsPlayer() && t.getPlayer() == p) {
						int currentCost = t.getCostToReach();
						// checking if visited (not default of 99)
						if (currentCost == 99) {
							t.setCostToReach(cost + 1);
							if (cost + 1 > p.getRemainingMA()) {
								t.goForIt();
							} else {
								t.setGoForIt(false);
							}
							queue.add(t);
						}
					}
				}
			}
		}
	}

	// An A star algorithm for Player to get from a to b, favouring avoiding tackle
	// zones and going for it
	// Penalties mean no longer gauranteed to find a route if involves many
	// penalties, so if fails to find one
	// repeats without penalties. If still fails, exception thrown
	public List<Tile> getOptimisedRoute(int playerId, int[] from, int[] goal, boolean withPenalties) {
		PlayerInGame p = getPlayerById(playerId);
		makeActivePlayer(p);
		actionCheck(p);
		addTackleZones(p);
		Tile origin = pitch[from[0]][from[1]];
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
				t.setWeightedDistance(10000);
				t.setTotalDistance(10000.0);
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
				// if visiting target square
				if (current.equals(target)) {
					return getTravelPath(p, origin, target);
				}

				List<Tile> neighbours = getNeighbours(current);
				for (Tile neighbour : neighbours) {
					if (!neighbour.isVisited()) {

						// Chebyshev distance. Straight moves made slightly cheaper cost as otherwise
						// creates diagonal move heavy routes, which to human looks odd
						double xDist = Math.abs((neighbour.getLocation()[0] - target.getLocation()[0]));
						double yDist = Math.abs((neighbour.getLocation()[1] - target.getLocation()[1]));
						double predictedDistance = 0.99 * (xDist + yDist) + (1 - 2 * 0.99) * Math.min(xDist, yDist);

						// Penalties to prevent invalid moves and prefer not entering tackle zones or
						// Going For It
						int movementToReach = current.getMovementUsed() + 1;
						int neighbourCost = neighbour.getPlayer() != null ? 10000 : 1;
						int noMovementPenalty = movementToReach > MA + 2 ? 10000 : 0;
						double goForItPenalty = 0;
						double tackleZonesPenalty = 0;
						if (withPenalties == true) {
							goForItPenalty = movementToReach > MA ? (movementToReach - MA) * 4 : 0;
							tackleZonesPenalty = Math.abs(neighbour.getTackleZones()) * 4;
						}

						double totalDistance = current.getWeightedDistance() + neighbourCost + goForItPenalty
								+ noMovementPenalty + tackleZonesPenalty + predictedDistance;
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
							if (priorityQueue.contains(neighbour)) {
								priorityQueue.remove(neighbour);
							}
							priorityQueue.add(neighbour);
						}
					}
				}
			}
		}
		if (withPenalties == true) {
			return getOptimisedRoute(playerId, from, goal, false);
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

	public List<jsonTile> jsonRoute(List<Tile> route, PlayerInGame p) {
		List<jsonTile> jsonRoute = new ArrayList<>();
		if (route.isEmpty()) {
			return new ArrayList<jsonTile>();
		}
		addTackleZones(p);
		int standingCost = 0;
		if (p.getStatus().equals("prone")) {
			standingCost = 3;
		}
		for (int i = 0; i < route.size(); i++) {
			Tile t = route.get(i);
			jsonTile jt = new jsonTile(t);
			// System.out.print("\n" + t.getLocation()[0] + " " + t.getLocation()[1]);
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
					jt.setDodgeRoll(calculateDodge(p, route.get(i)));
				}
			}
			if (t.containsBall()) {
				System.out.print(" Pick Up Ball: " + calculatePickUpBall(p, t) + "+");
				jt.setPickUpBallRoll(calculatePickUpBall(p, t));
			}
			jsonRoute.add(jt);
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
				totalRoute.addAll(getOptimisedRoute(p.getId(), origin.getLocation(), i, true));
				origin = totalRoute.get(totalRoute.size() - 1);
				totalRoute.remove(totalRoute.size() - 1); // removes duplicate tiles
				p.setRemainingMA(startingMA - (totalRoute.size()));
				// System.out.println("remaining MA: " + p.getRemainingMA());
			}
			totalRoute.addAll(getOptimisedRoute(p.getId(), origin.getLocation(), goal, true));
		} catch (Exception e) {
			System.out.println("Can't reach here");
			return new ArrayList<Tile>();
		} finally {
			p.setRemainingMA(startingMA);
			// queue.add(() -> getRouteWithWaypoints((PlayerInGame) p, waypoints, goal));
		}
		return totalRoute;
	}

	public void blitzAction(PlayerInGame attacker, PlayerInGame defender, List<int[]> route) {
		actionCheck(attacker);
		if (attacker.getTeamIG().hasBlitzed()) {
			throw new IllegalArgumentException("Can only attempt blitz once per turn");
		}
		attacker.getTeamIG().setBlitzed(true); // counts as blitzed even if movement fails, etc.
		sender.sendBlitzUsed(game.getId(), attacker.getTeam());
		blitz = new Runnable() {
			@Override
			public void run() {
				if (attacker.getStatus().equals("standing")) {
					attacker.setActionOver(false);// only if movement was successful
					carryOutBlock(attacker.getId(), defender.getId(), route.get(route.size() - 1), false,
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
		if (target.containsPlayer() == false) {
			throw new IllegalArgumentException("No player in that square");
		}
		if (target.containsPlayer() && target.getPlayer().getTeam() == attacker.getTeam()) {
			throw new IllegalArgumentException("Can't attack player on your team");
		}
		if (!Arrays.equals(target.getLocation(), goal)) {
			throw new IllegalArgumentException("Invalid target");
		}
		List<Tile> route = calculateBlitzRoute(attacker, waypoints, goal);
		List<jsonTile> jRoute = jsonRoute(route, attacker);
		PlayerInGame opponent = target.getPlayer();
		int[] block = calculateBlock(attacker, route.get(route.size() - 1), opponent);
		System.out.println("Blitz: " + block[0] + " dice, " + (block[1] == attacker.getTeam() ? "attacker" : "defender")
				+ " chooses");
		int routeMACost;
		if (route.isEmpty()) {
			routeMACost = 0;
		} else {
			routeMACost = jRoute.size() - 1 + (jRoute.get(0).getStandUpRoll() != null ? 3 : 0);
		}
		int[][] attLocations = getJsonFriendlyAssists(attacker, route.get(route.size() - 1), opponent,
				opponent.getTile());
		int[][] defLocations = getJsonFriendlyAssists(opponent, opponent.getTile(), attacker,
				route.get(route.size() - 1));
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
			route = getOptimisedRoute(attacker.getId(), attacker.getLocation(), goal, true);
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
		int[] assists = calculateAssists(attacker, attacker.getTile(), defender);
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
		int[] assists = calculateAssists(attacker, attacker.getTile(), defender);
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
		} else {
			turnover();
		}
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
		int roll = calculateDodge(p, to);
		int result = diceRoller(1, 6)[0];
		System.out.println("Needed " + roll + "+" + " Rolled: " + result);
		rollType = "DODGE";
		rollNeeded = roll;
		rolled.clear();
		rolled.add(result);
		if (p.hasSkill("Stunty")) {
			sender.sendSkillUsed(game.getId(), p.getId(), p.getName(), p.getTeam(), "Stunty Dodge");
		}
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

	public int calculateDodge(PlayerInGame p, Tile to) {
		addTackleZones(p);
		int AG = p.getAG();
		int modifier = 0;
		if (!p.hasSkill("Stunty")) {
			modifier = to.getTackleZones();
		}
		int result = 7 - AG - 1 - modifier;
		if (result <= 1)
			result = 2; // roll of 1 always fails, no matter what
		if (result > 6)
			result = 6; // roll of 6 always passes, no matter what
		return result;
	}

	public int[] blockAction(PlayerInGame attacker, PlayerInGame defender) {
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
	public void blockChoiceAction(int blockChoice, PlayerInGame attacker, PlayerInGame defender) {
		makeActivePlayer(attacker);
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
				attacker.setStatus("prone");
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
						System.out.println("In knock");
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
			pushAction(attacker, defender, false);
		}
		if (blitz == null) {
			endOfAction(attacker);
		} else {
			System.out.println("Blitzer is here:" + attacker.getTile());
		}
		System.out.println(attacker.getStatus());
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
		int[] assists = calculateAssists(attacker, from, defender);
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

	public void pushAction(PlayerInGame attacker, PlayerInGame defender, boolean chained) {
		List<Tile> push = calculatePushOptions(attacker, defender);
		Tile target = defender.getTile();
		Tile origin = attacker.getTile();
		if (chained == false) {
			Runnable follow = new Runnable() {
				@Override
				public void run() {
					System.out.println("start of follow up runnable");
					if (isFollowUp == true) {
						attacker.getTile().removePlayer();
						target.addPlayer(attacker);
						System.out.println(attacker.getName() + " follows up to " + target.getLocation()[0] + " "
								+ target.getLocation()[1]);
						sender.sendPushResult(game.getId(), attacker.getId(), attacker.getName(), origin.getLocation(),
								target.getLocation(), "FOLLOW");
						if (attacker.isHasBall()) {
							System.out.println("checking for touchdown");
							if ((target.getLocation()[0] == 0 && attacker.getTeamIG() == team2)
									|| target.getLocation()[0] == 25 && attacker.getTeamIG() == team1) {
								touchdown(attacker);
							}
						}
					}
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
			Runnable task = new Runnable() {

				@Override
				public void run() {
					pushOffPitch(attacker, defender);
				}
			};
			taskQueue.addFirst(task);
			sender.requestFollowUp(game.getId(), attacker.getId(), defender.getId(), attacker.getLocation(),
					defender.getLocation(), "Off Pitch", activeTeam.getId());
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
					System.out.println("in carry out runnable 1");
					carryOutPush(attacker.getId(), defender.getId(), attacker.getLocation(), defender.getLocation(),
							push);
				}
			};
			taskQueue.addFirst(task);

			int userToChoose = activeTeam.getId();
			if (defender.hasSkill("Side Step") && defender.getStatus() == "standing"
					&& defender.getTeam() != activeTeam.getId() && !push.get(0).containsPlayer()) {
				userToChoose = defender.getTeam();
				sender.sendSkillUsed(game.getId(), defender.getId(), defender.getName(), defender.getTeam(),
						"Side Step");
			}
			sender.requestPushChoice(game.getId(), attacker.getId(), defender.getId(), attacker.getLocation(),
					defender.getLocation(), jPush, userToChoose);
		}
	}

	public void carryOutPush(int pusher, int pushed, int[] pusherLocation, int[] pushedLocation, List<Tile> options) {
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
					if (p.isHasBall()) {
						System.out.println("checking for touchdown");
						if ((target[0] == 0 && p.getTeamIG() == team2) || target[0] == 25 && p.getTeamIG() == team1) {
							touchdown(p);
							return;
						}
					}
					if (!taskQueue.isEmpty()) {
						taskQueue.pop().run();
					} else {
						sendBlockSuccess(getPlayerById(pusher), getPlayerById(pushed));
					}
				}
			};
			taskQueue.addFirst(task); // final push movement must be first to occur
			System.out.println("in chain push");
			pushAction(getPlayerById(pushed), pushChoice.getPlayer(), true);
		} else {
			Runnable task4 = new Runnable() {
				@Override
				public void run() {
					System.out.println("in final push");
					origin.removePlayer();
					pushChoice.addPlayer(p);
					sender.sendPushResult(game.getId(), pushed, p.getName(), pushedLocation, runnableLocation[0],
							"PUSH");
					if (p.isHasBall()) {
						System.out.println("checking for touchdown");
						if ((runnableLocation[0][0] == 0 && p.getTeamIG() == team2)
								|| runnableLocation[0][0] == 25 && p.getTeamIG() == team1) {
							touchdown(p);
						}
					}
					if (!taskQueue.isEmpty()) {
						taskQueue.pop().run();
					} else {
						sendBlockSuccess(getPlayerById(pusher), getPlayerById(pushed));
					}
				}
			};
			taskQueue.addFirst(task4);
			sender.requestFollowUp(game.getId(), p2.getId(), p.getId(), p2.getLocation(), p.getLocation(), "normal",
					activeTeam.getId());
		}
	}

	public void followUpChoice(boolean followUp) {
		isFollowUp = followUp;
		String choice = followUp ? "follow" : "not";
		sender.sendFollowUpChoice(game.getId(), activeTeam.getName(), choice);
		taskQueue.pop().run();
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
				if (!defender.hasSkill("Side Step") && !t.containsPlayer()) {
					options.add(t);
				}
			}
			if (defender.hasSkill("Side Step") && defender.getStatus() == "standing" && !t.containsPlayer()) {
				options.add(t);
			}
		}
		return options.size() > 0 ? options : noEmptyOptions;
	}

	public void pushOffPitch(PlayerInGame pusher, PlayerInGame pushed) {
		System.out.println(pushed.getName() + " was pushed into the crowd and gets beaten!");
		int[] direction = new int[] { pushed.getLocation()[0] - pusher.getLocation()[0],
				pushed.getLocation()[1] - pusher.getLocation()[1] };
		int[] target = new int[] { pushed.getLocation()[0] + direction[0], pushed.getLocation()[1] + direction[1] };
		sender.sendPushResult(game.getId(), pushed.getId(), pushed.getName(), pushed.getLocation(), target, "OFFPITCH");
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
			Runnable ballOff = new Runnable() {
				@Override
				public void run() {
					ballOffPitch(origin);
					if (taskQueue.isEmpty()) {
						sendBlockSuccess(pusher, pushed);
					} else {
						taskQueue.pop().run();
					}
				}
			};
			taskQueue.add(ballOff); // throw in needs to happen after follow up and knockdown
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
		} else {
			sendBlockSuccess(pusher, pushed);
		}
	}

	public void touchdown(PlayerInGame p) {
		phase = "kickOff";
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
		if (team1.getTurn() == 8 && team2.getTurn() == 8 || team1.getTurn() == 16 && team2.getTurn() == 16) {
			endOfHalf();
		} else {
			kickOff(tg);
		}
		gameRepo.save(game);
	}

	public void knockDown(PlayerInGame p) {
		p.setProne();
		Tile location = p.getTile();
		int armour = p.getAV();
		int[] rolls = diceRoller(2, 6);
		int total = rolls[0] + rolls[1];
		sender.sendArmourRoll(game.getId(), p.getId(), p.getName(), armour, rolls,
				total > armour ? "armour was broken" : "armour held so is just knocked down");
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
		if (p.hasSkill("Stunty")) {
			total += 1;
			sender.sendSkillUsed(game.getId(), p.getId(), p.getName(), p.getTeam(), "Stunty Injury");
		}
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
				System.out.println(p.getName() + " is injured and sent to the injury box for the rest of the match");
				outcome = "injured and sent to the injury box for the rest of the match";
				p.setStatus("injured");
				p.getTeamIG().addToInjured(p);
			}
			p.getTile().removePlayer();
		}
		sender.sendInjuryRoll(game.getId(), p.getId(), p.getName(), rolls, p.getStatus(), p.getLocation(), outcome);
	}

	public void carryOutStandUp(int playerId) {
		standUpAction(getPlayerById(playerId), false);
	}

	public boolean standUpAction(PlayerInGame player, boolean inMovement) {
		if (player.getStatus() != "prone") {
			throw new IllegalArgumentException("Can't stand up a player that isn't prone");
		}
		makeActivePlayer(player);
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
		player.setActedThisTurn(true);
		System.out.println(player.getName() + " stood up");
		String end = "Y";
		if (inMovement == true) {
			end = "N";
		}
		sender.sendStandUpAction(game.getId(), player.getId(), player.getName(), end);
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
		if (position[0] >= 0 && position[0] < 26 && position[1] >= 0 && position[1] < 15) {
			Tile target = pitch[position[0]][position[1]];
			System.out.println("Ball scattered to: " + position[0] + " " + position[1]);
			sender.sendBallScatterResult(game.getId(), origin.getLocation(), position);
			if (times > 1) {
				scatterBall(target, times - 1);
				return;
			}
			if (target.containsPlayer() && !(phase == "kick" && target.getPlayer().getTeam() == activeTeam.getId())) {
				if (target.getPlayer().isHasTackleZones()) { // will need to make this more specific to catching
					catchBallAction(target.getPlayer(), false);
				} else {
					scatterBall(target, 1); // if player can't catch, will scatter again
				}
			} else {
				target.setContainsBall(true);
				if (inPassOrHandOff == true) {
					System.out.println("bad throw onto empty square");
					inPassOrHandOff = false;
					turnover();
				} else if (inTurnover == true) {
					turnover();
				}
				if (phase == "kick") {
					checkForTouchBack();
				}

			}
		} else {
			sender.sendBallScatterResult(game.getId(), origin.getLocation(), position);
			ballOffPitch(origin);
		}
	}

	public void catchBallAction(PlayerInGame player, boolean accuratePass) {
		int needed = calculateCatch(player, accuratePass);
		rerollOptions.clear();
		int roll = diceRoller(1, 6)[0];
		rolled.clear();
		rolled.add(roll);
		rollType = "CATCH";
		System.out.println(player.getName() + " tries to catch the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " caught the ball!");
			sender.sendRollResult(game.getId(), player.getId(), player.getName(), "CATCH", needed, rolled, "success",
					player.getLocation(), player.getLocation(), null, player.getTeam(), "Y", false);
			player.setHasBall(true);
			inPassOrHandOff = false;
			if ((player.getLocation()[0] == 0 && player.getTeamIG() == team2)
					|| (player.getLocation()[0] == 25 && player.getTeamIG() == team1)) {
				touchdown(player);
				return;
			}
			if (phase == "main game" && player.getTeamIG() != activeTeam) {
				turnover();
				return;
			} else if (phase == "kick") {
				checkForTouchBack();
				return;
			}
		} else {
			System.out.println(player.getName() + " failed to catch the ball!");
			rerollOptions = determineRerollOptions("CATCH", player.getId(),
					new int[][] { player.getLocation(), player.getLocation() });
			if (awaitingReroll != null && player.getId() == Integer.parseInt(awaitingReroll[2])
					&& runnableLocation[0] == player.getLocation()) {
				rerollOptions.clear();
			}
			String end = "Y";
			if (!rerollOptions.isEmpty()) {
				runnableLocation = new int[][] { player.getLocation(), player.getLocation() };
				awaitingReroll = new String[] { "Y", "CATCH", "" + player.getId() };
				end = "N";
				Runnable task = new Runnable() {
					@Override
					public void run() {
						taskQueue.pop();
						catchBallAction(player, accuratePass);
					}
				};
				taskQueue.addFirst(task);
				Runnable task2 = new Runnable() { // for if choose not to reroll
					@Override
					public void run() {
						System.out.println("in no reroll");
						scatterBall(player.getTile(), 1);
					}
				};
				taskQueue.add(task2);
			}
			sender.sendRollResult(game.getId(), player.getId(), player.getName(), "CATCH", needed, rolled, "failed",
					player.getLocation(), player.getLocation(), rerollOptions, player.getTeam(), end, false);
			if (rerollOptions.isEmpty()) {
				awaitingReroll = new String[] { "N", "CATCH", "" + player.getId() };
				scatterBall(player.getTile(), 1);
				if (phase != "kick") {
					Tile scatteredTo = ballLocationCheck();
					if (!scatteredTo.containsPlayer() && inPassOrHandOff == true
							|| scatteredTo.containsPlayer() && scatteredTo.getPlayer().getTeamIG() != activeTeam) {
						turnover(); // only a turnover if ball is not caught by player on active team before comes
									// to
									// rest
					}
				}
			}
		}
	}

	public void interceptBallAction(int[] source, PlayerInGame player, boolean reroll) {
		int needed = calculateInterception(player);
		int roll = diceRoller(1, 6)[0];
		rollType = "CATCH";
		ArrayList<Integer> rolledLocal = new ArrayList<>();
		rolledLocal.add(roll); // local rather than directly access global variable, due to sending intercept
								// result after throw (despite intercept happening first)
		System.out.println(player.getName() + " tries to intercept the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		if (roll >= needed) {
			System.out.println(player.getName() + " intercepted the ball!");
			player.setHasBall(true);
			taskQueue.pop().run();
//			if(reroll == true) {
//				taskQueue.pop().run();
//			}
			sender.sendRollResult(game.getId(), player.getId(), player.getName(), "INTERCEPT", needed, rolledLocal,
					"success", source, player.getLocation(), rerollOptions, player.getTeam(), "Y", reroll);
			turnover();
			return;
		} else {
			System.out.println(player.getName() + " failed to intercept the ball!");
			List<String> options = determineRerollOptions("CATCH", player.getId(),
					new int[][] { source, player.getLocation() });
			runnableLocation = new int[][] { source, player.getLocation() };
			awaitingReroll = new String[] { "Y", "CATCH", "" + player.getId() };
			if (options.isEmpty()) {
				taskQueue.pop(); // get rid of intercepted throw result;
				Runnable task = new Runnable() {
					@Override
					public void run() {
						sender.sendRollResult(game.getId(), player.getId(), player.getName(), "INTERCEPT", needed,
								rolledLocal, "failed", source, player.getLocation(), null, player.getTeam(), "Y",
								reroll);
					}
				};
				taskQueue.add(task);
				taskQueue.pop().run();
			}
			if (!options.isEmpty()) { // will automatically use reroll if available, as no reason not to (can only be
										// a skill)
				Runnable task2 = new Runnable() {

					@Override
					public void run() {
						sender.sendRollResult(game.getId(), player.getId(), player.getName(), "INTERCEPT", needed,
								rolledLocal, "failed", source, player.getLocation(), null, player.getTeam(), "N",
								reroll);
						sender.sendSkillUsed(game.getId(), player.getId(), player.getName(), player.getTeam(), "Catch");
						taskQueue.pop().run();
					}

				};
				taskQueue.add(task2);
				interceptBallAction(source, player, true);
			}
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
		taskQueue.clear();
		makeActivePlayer(thrower);
		int[] details = calculateThrow(thrower, thrower.getTile(), target);
		int needed = details[0];
		int modifier = details[1];
		inPassOrHandOff = true;
		int roll = diceRoller(1, 6)[0];
		rollType = "THROW";
		rolled.clear();
		rolled.add(roll);
		String outcome = "";
		System.out.println(thrower.getName() + " tries to throw the ball");
		System.out.println("Needs a roll of " + needed + "+. Rolled " + roll);
		thrower.setHasBall(false);
		sender.sendHasThrown(game.getId(), activeTeam.getId());
		if (thrower.hasSkill("Stunty")) {
			sender.sendSkillUsed(game.getId(), thrower.getId(), thrower.getName(), thrower.getTeam(), "Stunty Pass");
		}
		if (roll < needed) {
			//System.out.println(thrower.getName() + " fumbled the ball!");
			rerollOptions = determineRerollOptions("THROW", thrower.getId(),
					new int[][] { thrower.getLocation(), target.getLocation() });
			if (reroll == true) {
				rerollOptions.clear();
			}
			String finalRoll = "N";
			if (rerollOptions.isEmpty()) {
				finalRoll = "Y";
				if(roll == 1 || (roll + modifier) <= 1) {
					outcome = "failed";
				} else {
					outcome = "badly";
				}
			} else {
				runnableLocation = new int[][] { thrower.getLocation(), target.getLocation() };
				awaitingReroll = new String[] { "Y", "THROW", "" + thrower.getId() };
				Runnable task = new Runnable() {
					@Override
					public void run() {
						passBallAction(thrower, target, true);
					}
				};
				taskQueue.add(task);
				Runnable task2;
				if (roll == 1 || (roll + modifier) <= 1) {// on a natural 1 or 1 after modifiers
					outcome = "failed";
					task2 = new Runnable() { // for if choose not to reroll
						@Override
						public void run() {
							System.out.println("in no reroll");
							inPassOrHandOff = false;
							inTurnover = true;
							scatterBall(thrower.getTile(), 1);
						}
					};
				} else {
					outcome = "badly";
					task2 = new Runnable() { // for if choose not to reroll
						@Override
						public void run() {
							continuePass(roll, needed, thrower, target, false);
						}
					};
				}
				taskQueue.add(task2);
			}
			sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled, outcome,
					thrower.getLocation(), target.getLocation(), rerollOptions, thrower.getTeam(), finalRoll, reroll);
			if (rerollOptions.isEmpty()) {
				inPassOrHandOff = false;
				inTurnover = true;
				scatterBall(thrower.getTile(), 1);
			}
		} else {
			if (interceptor != null) {
				rollType = "INTERCEPT";
				Runnable task2 = new Runnable() {

					@Override
					public void run() {
						sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled,
								"intercepted", thrower.getLocation(), target.getLocation(), null, thrower.getTeam(),
								"N", reroll);
					}
				};
				taskQueue.add(task2);

				Runnable task3 = new Runnable() {
					@Override
					public void run() {
						continuePass(roll, needed, thrower, target, reroll);
					}
				};
				taskQueue.add(task3);
				interceptBallAction(thrower.getLocation(), interceptor, false);
			} else {
				continuePass(roll, needed, thrower, target, reroll);
			}
		}
	}

	public void continuePass(int roll, int needed, PlayerInGame thrower, Tile target, boolean reroll) {
		rolled.clear();
		rolled.add(roll);
		if (roll >= needed) {
			System.out.println(thrower.getName() + " threw the ball accurately!");
			thrower.setHasBall(false);
			String end = "Y";
			if (target.containsPlayer() && target.getPlayer().isHasTackleZones()) {
				end = "N";
			}
			sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled, "success",
					thrower.getLocation(), target.getLocation(), null, thrower.getTeam(), end, false);
			if (taskQueue.size() > 0) {
				taskQueue.pop().run(); // send intercept failure if happened
			}
			if (target.containsPlayer()) {
				if (target.getPlayer().isHasTackleZones()) {
					catchBallAction(target.getPlayer(), true);
				} else {
					scatterBall(target, 1);
				}
			} else {
				target.setContainsBall(true);
				turnover();
			}
		} else {
			System.out.println(thrower.getName() + " threw the ball badly");
			sender.sendRollResult(game.getId(), thrower.getId(), thrower.getName(), "THROW", needed, rolled, "badly",
					thrower.getLocation(), target.getLocation(), null, thrower.getTeam(), "Y", false);
			if (taskQueue.size() > 0) {
				taskQueue.pop().run(); // send intercept failure if happened
			}
			scatterBall(target, 3);
		}
		thrower.getTeamIG().setPassed(true);
		endOfAction(thrower);
	}

	public void handOffBallAction(PlayerInGame player, Tile target, PlayerInGame targetPlayer) {
		actionCheck(player);
		makeActivePlayer(player);
		inPassOrHandOff = true;
		sender.sendHasHandedOff(game.getId(), activeTeam.getId());
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
		sender.sendHandOffAction(game.getId(), player.getId(), player.getLocation(), player.getName(),
				target.getLocation(), targetPlayer.getId(), targetPlayer.getName());
		System.out.println(player.getName() + " hands off the ball to " + target.getPlayer().getName());
		player.setHasBall(false);
		endOfAction(player);
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

	public int calculateHandOff(PlayerInGame p, Tile target) {
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
//		addTackleZones(thrower);  this is handled within agility calculation
//		modifier += from.getTackleZones();
		if (thrower.hasSkill("Stunty")) {
			modifier -= 1;
		}
		System.out.println(modifier);
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

		for (; n > 0; n--) {
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
		if (phase == "kick") {
			getTouchBack(activeTeam == team1 ? team2 : team1);
			return;
		}
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
				sender.sendThrowIn(game.getId(), position, destination);
				destination[0] -= direction[0];
				destination[1] -= direction[1];
				ballOffPitch(pitch[destination[0]][destination[1]]);
				return;
			}
		}
		Tile target = pitch[destination[0]][destination[1]];
		System.out.println("Ball thrown to square " + destination[0] + " " + destination[1]);
		sender.sendThrowIn(game.getId(), position, destination);
		if (target.containsPlayer()) {
			if (target.getPlayer().isHasTackleZones()) {
				catchBallAction(target.getPlayer(), false);
			} else {
				scatterBall(target, 1);
			}
		} else {
			scatterBall(target, 1);
		}
	}

	public synchronized void addTackleZones(PlayerInGame player) {
		resetTackleZones();
		List<PlayerInGame> opponents;
		opponents = player.getTeamIG() == team1 ? new ArrayList<>(team2.getPlayersOnPitch())
				: new ArrayList<>(team1.getPlayersOnPitch());
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

	public int[] calculateAssists(PlayerInGame attacker, Tile attackerLocation, PlayerInGame defender) {
		List<PlayerInGame> attSupport = getAssists(attacker, attackerLocation, defender, defender.getTile());
		List<PlayerInGame> defSupport = getAssists(defender, defender.getTile(), attacker, attackerLocation);
		return new int[] { attSupport.size(), defSupport.size() };
	}

	public List<PlayerInGame> getAssists(PlayerInGame p1, Tile p1Location, PlayerInGame p2, Tile p2Location) {
		Tile p1Origin = p1.getTile();
		Tile p2Origin = p2.getTile();
		if (!Arrays.equals(p1Origin.getLocation(), p1Location.getLocation())) {
			p1Location.addPlayer(p1);
		}
		if (!Arrays.equals(p2Origin.getLocation(), p2Location.getLocation())) {
			p2Location.addPlayer(p2);
		}
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
		// if(!Arrays.equals(p1Origin.getLocation(), p1Location.getLocation())) {
		p1Location.removePlayer();
		p1Origin.addPlayer(p1);
		// }
		// if(!Arrays.equals(p2Origin.getLocation(), p2Location.getLocation())) {
		p2Location.removePlayer();
		p2Origin.addPlayer(p2);
		// }
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
		if (p.getActionOver() == true && blitz == null) {
			System.out.println(p.getName());
			throw new IllegalArgumentException("That player's action has finished for this turn");
		}
		if (p.getStatus() == "stunned") {
			throw new IllegalArgumentException("A stunned player cannot act");
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
		if (ballLocation != null && pitch[ballLocation[0]][ballLocation[1]].containsPlayer()) {
			ball = null;
		}
		sender.sendGameStatus(game.getId(), activeTeam.getId(), activeTeam.getName(), team1, team2,
				game.getTeam1Score(), game.getTeam2Score(), ball, phase);
	}

	public void sendRoute(int playerId, int[] from, int[] target, int teamId) {
		makeActivePlayer(getPlayerById(playerId));
		List<jsonTile> route = jsonRoute(getOptimisedRoute(playerId, from, target, true), getPlayerById(playerId));
		int routeMACost;
		if (route.isEmpty()) {
			routeMACost = 0;
		} else {
			routeMACost = route.size() - 1 + (route.get(1).getStandUpRoll() != null ? 3 : 0);
		}
		sender.sendRoute(game.getId(), teamId, playerId, route, routeMACost);
	}

	public void sendWaypointRoute(int playerId, int[] target, List<int[]> waypoints, int teamId) {
		makeActivePlayer(getPlayerById(playerId));
		List<jsonTile> route = jsonRoute(getRouteWithWaypoints(playerId, waypoints, target), getPlayerById(playerId));
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
		makeActivePlayer(p);
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
		if (actionsNeeded > 0) {
			continueAction(playerId, route, jsonMoved, teamId);
		} else {
			if (p.isHasBall()) {
				System.out.println("checking for touchdown");
				if ((route.get(jsonMoved.size() - 1)[0] == 0 && p.getTeamIG() == team2)
						|| route.get(jsonMoved.size() - 1)[0] == 25 && p.getTeamIG() == team1) {
					touchdown(p);
					return;
				}
			}
//			if (taskQueue.size() > 0) {
//				taskQueue.pop().run();
			//} else 
				if (blitz != null) {
				blitz.run();
			}
		}
	}

	public void continueAction(int playerId, List<int[]> route, List<jsonTile> jsonMoved, int teamId) {
		boolean result;
		System.out.println("in continue Action");
		List<int[]> remaining = route.subList(jsonMoved.size(), route.size()); // sublist method is exclusive of final index so must be size not size -1
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
		System.out.println("actions needed: " + actionsNeeded);
		if ((Arrays.equals(runnableLocation[1], route.get(route.size() - 1))
				|| Arrays.equals(runnableLocation[0], route.get(route.size() - 1)))
				&& (awaitingReroll == null || awaitingReroll[0] == "N") && actionsNeeded <= 0) {
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
			if (rollType == "PICKUPBALL") {
				if ((target[0] == 0 && p.getTeamIG() == team2) || target[0] == 25 && p.getTeamIG() == team1) {
					touchdown(p);
					return;
				}
			}
			if (actionsNeeded > 0) {
				continueAction(playerId, route, jsonMoved, teamId);
			} else {
				if (p.isHasBall()) {
					System.out.println("checking for touchdown");
					if ((target[0] == 0 && p.getTeamIG() == team2) || target[0] == 25 && p.getTeamIG() == team1) {
						touchdown(p);
						return;
					}
				}
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
				inTurnover = true;
				scatterBall(ballToScatter, 1);
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
			if (!standUpAction(p, true)) {
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
		System.out.println("in determine rerolls");
		List<String> results = new ArrayList<>();
		if (action != "BLOCK" && awaitingReroll != null && runnableLocation != null
				&& Arrays.equals(location[0], runnableLocation[0]) && Arrays.equals(location[1], runnableLocation[1])
				&& playerId == Integer.parseInt(awaitingReroll[2]) && action == awaitingReroll[1]) { // means in a
																										// reroll -
																										// can't reroll
																										// something
																										// more than
																										// once
			System.out.println("the same");
			return results;
		}

		PlayerInGame p = getPlayerById(playerId);
		if (phase == "main game" && p.getTeamIG() == activeTeam && !activeTeam.hasRerolled()
				&& activeTeam.getRemainingTeamRerolls() > 0) {
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
			if (p.hasSkill("Catch")) {
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
			if (rollType == "BLOCK") {
				Runnable task = taskQueue.pop();
				taskQueue.pop();
				task.run();
				return;
			}
			taskQueue.pop().run();
			if (rollType == "INTERCEPT" || rollType == "THROW" || rollType == "CATCH") {
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
				} else {
					if (p.isHasBall()) {
						System.out.println("checking for touchdown");
						if ((runnableLocation[1][0] == 0 && p.getTeamIG() == team2)
								|| runnableLocation[1][0] == 25 && p.getTeamIG() == team1) {
							touchdown(p);
						}
					}
				}
				return;
			}
		}
		if (rollType == "DODGE" || rollType == "GFI") {
			knockDown(p);
		} else if (rollType == "PICKUPBALL") {
			ballToScatter = pitch[runnableLocation[1][0]][runnableLocation[1][1]];
		} else if (rollType == "BLOCK" || rollType == "THROW" || rollType == "CATCH") {
			System.out.println("don't reroll");
			taskQueue.pop();
			taskQueue.pop().run();
			return;
		}
		if (ballToScatter != null) {
			inTurnover = true;
			scatterBall(ballToScatter, 1);
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
		int[][] attLocations = getJsonFriendlyAssists(attacker, attacker.getTile(), defender, defender.getTile());
		int[][] defLocations = getJsonFriendlyAssists(defender, defender.getTile(), attacker, attacker.getTile());
		sender.sendBlockInfo(game.getId(), player, opponent, location, defender.getLocation(), block, attLocations,
				defLocations, team);
	}

	public int[][] getJsonFriendlyAssists(PlayerInGame attacker, Tile attackerLocation, PlayerInGame defender,
			Tile defenderLocation) {
		List<PlayerInGame> support = getAssists(attacker, attackerLocation, defender, defenderLocation);
		// just send assist locations to save amount of data sent
		int[][] assistLocations = new int[support.size()][2];
		for (int i = 0; i < support.size(); i++) {
			assistLocations[i] = support.get(i).getLocation();
		}
		return assistLocations;
	}

	public void carryOutBlock(int player, int opponent, int[] location, boolean reroll, int team) {
		isFollowUp = false;
		PlayerInGame attacker = getPlayerById(player);
		makeActivePlayer(attacker);
		PlayerInGame defender = getPlayerById(opponent);
		int[] details = blockAction(attacker, defender);
		int[][] attLocations = getJsonFriendlyAssists(attacker, attacker.getTile(), defender, defender.getTile());
		int[][] defLocations = getJsonFriendlyAssists(defender, attacker.getTile(), attacker, attacker.getTile());
		runnableLocation = new int[][] { location, defender.getLocation() };
		if (rerollOptions.size() > 0) {
			Runnable task = new Runnable() {
				@Override
				public void run() {
					System.out.println("in block reroll");
					awaitingReroll = null;
					carryOutBlock(player, opponent, location, true, team);
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

	public void carryOutBlockChoice(int diceChoice, int player, int opponent, int team) {
		getPlayerById(player).setActedThisTurn(true);
		sender.sendBlockDiceChoice(game.getId(), player, opponent, rolled.get(diceChoice),
				team == team1.getId() ? team1.getName() : team2.getName(), team);
		System.out.println("dice choice: " + diceChoice);
		// System.out.println("rolled: " + rolled.get(0));
		// System.out.println("rolled: " + rolled.get(1));
		System.out.println("chosen: " + rolled.get(diceChoice));
		blockChoiceAction(rolled.get(diceChoice) - 1, getPlayerById(player), getPlayerById(opponent));
		// blockChoiceAction(1, getPlayerById(player), getPlayerById(opponent),
		// followUp); // need to sort out follow up

	}

	public void carryOutPushChoice(int[] choice) {
		System.out.println("In carry out push choice");
		System.out.println(taskQueue.size());
		runnableLocation = new int[][] { choice };
		taskQueue.pop().run();
	}

	public void carryOutBlitz(Integer player, Integer opponent, List<int[]> route, int[] target, int team) {
		blitzAction(getPlayerById(player), getPlayerById(opponent), route);
	}

	public void sendBlockSuccess(PlayerInGame attacker, PlayerInGame defender) {
		if (blitz != null && attacker.getStatus() == "standing") {
			attacker.setActionOver(false);
			attacker.setActedThisTurn(true);
			makeActivePlayer(attacker);
		} else {
			attacker.setActionOver(true);
		}
		sender.sendBlockSuccess(game.getId(), attacker.getId(), defender.getId(), blitz != null);
		blitz = null;
		taskQueue.clear();
	}

	public ArrayList<String> getPossibleActions(PlayerInGame player) {
		turnedOver = false;
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
		if (!activeTeam.hasBlitzed() && player.getActedThisTurn() == false && player.getRemainingMA() > -3) {
			actions.add("blitz");
		}
		if (player.getRemainingMA() > -2) {
			actions.add("move");
		}
		if (player.isHasBall()) {
			if (!activeTeam.hasPassed()) {
				actions.add("throw");
			}
			if (!activeTeam.hasHandedOff()) {
				boolean target = false;
				for (Tile t : player.getTile().getNeighbours()) {
					if (t.containsPlayer()) {
						target = true;
					}
				}
				if (target == true) {
					actions.add("handOff");
				}
			}
		}
		addTackleZones(player);
		if (player.getStatus() == "standing" && player.getTile().getTackleZones() != 0
				&& player.getActedThisTurn() == false) {
			actions.add("block");
		}
		if (actions.isEmpty()) {
			actions.add("None");
		}
		inTurnover = false;
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
		interceptors = calculatePossibleInterceptors(calculateThrowTiles(p, p.getTile(), goal), p);
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
		if (goal.containsPlayer() && goal.getPlayer().isHasTackleZones()) {
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
		if (activePlayer == null) {
			activePlayer = thrower;
		} else if (activePlayer.getActedThisTurn() == true && activePlayer != thrower) {
			endOfAction(activePlayer);
			activePlayer = thrower;
		}
		actionCheck(thrower);
		if (!thrower.isHasBall()) {
			throw new IllegalArgumentException("Player doesn't have the ball");
		}
		if (thrower.getTeamIG().hasPassed()) {
			throw new IllegalArgumentException("Can only attempt pass once per turn");
		}
		Tile goal = pitch[target[0]][target[1]];
		List<Tile> path = calculateThrowTiles(thrower, thrower.getTile(), goal);
		interceptors = calculatePossibleInterceptors(path, thrower);
		if (interceptors.size() > 1) {
			Runnable task = new Runnable() {
				@Override
				public void run() {
					passBallAction(thrower, goal, false);
				}
			};
			taskQueue.add(task);
			ArrayList<Integer> interceptIds = new ArrayList<>();
			ArrayList<jsonTile> interceptLocations = new ArrayList<>();
			for (PlayerInGame p : interceptors) {
				interceptIds.add(p.getId());
				jsonTile jt = new jsonTile();
				jt.setPosition(p.getLocation());
				interceptLocations.add(jt);
			}
			sender.requestInterceptor(game.getId(), interceptIds, interceptLocations, interceptors.get(0).getTeam());
			return;
		} else if (interceptors.size() == 1) {
			interceptor = interceptors.get(0);
		}
		passBallAction(getPlayerById(player), goal, false);
	}

	public void sendHandOffDetails(Integer player, int[] target, Integer opponent, int team) {
		PlayerInGame p = getPlayerById(player);
		Tile goal = pitch[target[0]][target[1]];
		int roll = calculateHandOff(p, goal);
		sender.sendHandOffDetails(game.getId(), roll, player, p.getLocation(), target, opponent, team);
	}

	public void carryOutHandOff(Integer player, int[] target, Integer opponent, int team) {
		handOffBallAction(getPlayerById(player), pitch[target[0]][target[1]], getPlayerById(opponent));
	}

	public void carryOutPlacement(Integer player, int[] location) {
		playerPlacement(getPlayerById(player), location);
	}

	public void benchPlayer(Integer player) {
		removePlayerFromPitch(getPlayerById(player));
	}

	public void endSetup(int team) {
		if (team != activeTeam.getId()) {
			throw new IllegalArgumentException("Not yours to end");
		}
		endTeamSetup(activeTeam);
	}

	public void resetGame() {
		game.setTeam1Score(0);
		game.setTeam2Score(0);
		team1 = new TeamInGame(game.getTeam1());
		team2 = new TeamInGame(game.getTeam2());
		taskQueue = new LinkedList<>();
		rolled = new ArrayList<>();
		runnableResults = new LinkedBlockingQueue<>();
		actionsNeeded = 0;
		half = 0;
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
		waitingForPlayers = true;
		phase = "pre-game";
		team1Joined = false;
		team2Joined = false;
	}

	public void actOnIntercept(int team, Integer player, int[] location) {
		PlayerInGame p = getPlayerById(player);
		if (!interceptors.contains(p)) {
			throw new IllegalArgumentException("Not a valid interceptor");
		}
		if (!Arrays.equals(p.getLocation(), location)) {
			throw new IllegalArgumentException("Interceptor not in that location");
		}
		interceptor = p;
		taskQueue.pop().run();
	}

	public void makeActivePlayer(PlayerInGame player) {
		if (activePlayer == null) {
			activePlayer = player;
		} else if ((activePlayer.getActedThisTurn() == true || activePlayer.getTeam() != activeTeam.getId())
				&& activePlayer != player) {
			endOfAction(activePlayer);
			activePlayer = player;
		}
	}

	public void autoSetupTeam(String type, int team) {
		if (team != activeTeam.getId()) {
			throw new IllegalArgumentException("Not you to choose");
		}
		if (activeTeam.getId() == team1.getId()) {
			System.out.println("in autosetup");
			for (PlayerInGame p : team1.getPlayersOnPitch()) {
				if (p.getTile() != null) {
					p.getTile().removePlayer();
				}
			}
			if (type.contains("offense")) {
				pitch[11][4].addPlayer(getPlayerById(1));
				pitch[11][1].addPlayer(getPlayerById(4));
				pitch[12][5].addPlayer(getPlayerById(11));
				pitch[9][11].addPlayer(getPlayerById(5));
				pitch[12][7].addPlayer(getPlayerById(9));
				pitch[12][8].addPlayer(getPlayerById(8));
				pitch[11][10].addPlayer(getPlayerById(7));
				pitch[12][9].addPlayer(getPlayerById(14));
				pitch[12][6].addPlayer(getPlayerById(13));
				pitch[11][13].addPlayer(getPlayerById(10));
				pitch[9][3].addPlayer(getPlayerById(12));
			} else if (type.contains("defense")) {
				pitch[7][7].addPlayer(getPlayerById(7));
				pitch[12][6].addPlayer(getPlayerById(13));
				pitch[12][7].addPlayer(getPlayerById(9));
				pitch[11][9].addPlayer(getPlayerById(5));
				pitch[12][8].addPlayer(getPlayerById(8));
				pitch[12][5].addPlayer(getPlayerById(11));
				pitch[11][4].addPlayer(getPlayerById(12));
				pitch[12][12].addPlayer(getPlayerById(14));
				pitch[12][1].addPlayer(getPlayerById(4));
				pitch[12][2].addPlayer(getPlayerById(1));
				pitch[12][13].addPlayer(getPlayerById(10));
			}
			List<PlayerInGame> copy = new ArrayList<>(team1.getReserves());
			for (PlayerInGame p : copy) {
				team1.addPlayerOnPitch(p);
			}
			for (PlayerInGame p : team1.getDugout()) {
				p.getTile().removePlayer();
			}
			for (PlayerInGame p : team1.getInjured()) {
				p.getTile().removePlayer();
			}
		} else {
			for (PlayerInGame p : team2.getPlayersOnPitch()) {
				if (p.getTile() != null) {
					p.getTile().removePlayer();
				}
			}
			if (type.contains("offense")) {
				pitch[14][4].addPlayer(getPlayerById(15));
				pitch[14][2].addPlayer(getPlayerById(22));
				pitch[23][9].addPlayer(getPlayerById(3));
				pitch[14][13].addPlayer(getPlayerById(17));
				pitch[13][7].addPlayer(getPlayerById(20));
				pitch[14][12].addPlayer(getPlayerById(2));
				pitch[14][1].addPlayer(getPlayerById(21));
				pitch[13][9].addPlayer(getPlayerById(18));
				pitch[13][5].addPlayer(getPlayerById(19));
				pitch[23][5].addPlayer(getPlayerById(16));
				pitch[14][10].addPlayer(getPlayerById(6));
			} else if (type.contains("defense")) {
				pitch[18][5].addPlayer(getPlayerById(22));
				pitch[13][10].addPlayer(getPlayerById(6));
				pitch[13][7].addPlayer(getPlayerById(20));
				pitch[18][9].addPlayer(getPlayerById(3));
				pitch[13][8].addPlayer(getPlayerById(2));
				pitch[13][12].addPlayer(getPlayerById(18));
				pitch[13][2].addPlayer(getPlayerById(19));
				pitch[13][4].addPlayer(getPlayerById(15));
				pitch[14][1].addPlayer(getPlayerById(16));
				pitch[14][13].addPlayer(getPlayerById(17));
				pitch[13][6].addPlayer(getPlayerById(21));
			}
			List<PlayerInGame> copy = new ArrayList<>(team2.getReserves());
			for (PlayerInGame p : copy) {
				if (p.getId() != 23) {
					team2.addPlayerOnPitch(p);
				}
			}
			for (PlayerInGame p : team2.getDugout()) {
				p.getTile().removePlayer();
			}
			for (PlayerInGame p : team2.getInjured()) {
				p.getTile().removePlayer();
			}
		}
		sender.sendSetupUpdate(game.getId(), activeTeam, activeTeam == team1 ? 1 : 2);
	}

	// just for testing
	public void setPhase(String phase) {
		this.phase = phase;
	}

}