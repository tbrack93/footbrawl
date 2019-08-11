package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.footbrawl.controller.GameMessageController;
import com.project.footbrawl.instance.MessageToClient;
import com.project.footbrawl.instance.TeamInGame;
import com.project.footbrawl.instance.jsonTile;

@Service
public class MessageSendingService {

	@Autowired
	GameMessageController controller;

	public MessageSendingService() {

	}

	public void sendTeamsInfo(int gameId, int teamId, TeamInGame team1, TeamInGame team2, String phase) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("TEAMS");
		message.setTeam1Name(team1.getName());
		message.setTeam2Name(team2.getName());
		message.setTeam1FullDetails(team1);
		message.setTeam2FullDetails(team2);
		message.setPhase(phase);
		controller.sendMessageToUser(gameId, teamId, message);
	}
	
	public void sendSetupRequest(int gameId, String teamName, int teamId) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("TEAMSETUP");
		message.setTeamName(teamName);
		message.setUserToChoose(teamId);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendSetupUpdate(int gameId, TeamInGame teamDetails, int team) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("SETUPUPDATE");
        if(team == 1) {
        	message.setTeam1FullDetails(teamDetails);
        } else {
        	message.setTeam2FullDetails(teamDetails);
        }
        message.setDescription(""+team);
        controller.sendMessageToBothUsers(gameId, message);
	}
	
	public void sendKickRequest(int gameId, int userToChoose, String teamName) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("KICKOFF");
        message.setUserToChoose(userToChoose);
        message.setTeamName(teamName);
		controller.sendMessageToBothUsers(gameId, message);
	}
	
	public void sendPossibleActions(int gameId, Integer player, int[] location, List<String> actions, int team) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("ACTIONS");
		message.setLocation(location);
		message.setPossibleActions(actions);
		controller.sendMessageToUser(gameId, team, message);
	}

	public void sendMovementInfoMessage(int gameId, int teamId, int playerId, List<jsonTile> squares) {
		System.out.println("creating message");
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("MOVEMENT");
		message.setPlayer(playerId);
		message.setSquares(squares);
		controller.sendMessageToUser(gameId, teamId, message);
	}
	
	public void sendRoute(int gameId, int teamId, int playerId, List<jsonTile> route, int routeMACost) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("ROUTE");
		message.setRouteMACost(routeMACost);
		message.setPlayer(playerId);
		message.setRoute(route);
		controller.sendMessageToUser(gameId, teamId, message);
	}

	public void sendRouteAction(int gameId, int playerId, List<jsonTile> route, String end) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("ROUTE");
		message.setPlayer(playerId);
		message.setRoute(route);
		message.setEnd(end);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendRollResult(int gameId, int playerId, String playerName, String rollType, int rollNeeded,
			List<Integer> rolled, String rollResult, int[] origin, int[] target, List<String> rerollOptions, int teamId,
			String end, boolean isReroll) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("ROLL");
		message.setRollType(rollType);
		message.setPlayer(playerId);
		message.setRollNeeded(rollNeeded);
		message.setRolled(rolled);
		message.setRollOutcome(rollResult);
		message.setPlayerName(playerName);
		message.setLocation(origin);
		message.setTarget(target);
		message.setUserToChoose(teamId);
		message.setRerollOptions(rerollOptions);
		message.setEnd(end);
		message.setReroll(isReroll);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendRollResult(int gameId, int playerId, String playerName, String rollType, int rollNeeded,
			List<Integer> rolled, String rollResult, int[] location, List<String> rerollOptions, int teamId,
			String end) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("ROLL");
		message.setRollType(rollType);
		message.setPlayer(playerId);
		message.setRollNeeded(rollNeeded);
		message.setRolled(rolled);
		message.setRollOutcome(rollResult);
		message.setPlayerName(playerName);
		message.setLocation(location);
		message.setUserToChoose(teamId);
		message.setRerollOptions(rerollOptions);
		message.setEnd(end);
		controller.sendMessageToBothUsers(gameId, message);
	}
	
    public void sendBallScatterResult(int gameId, int[] origin, int[] target) {
    	MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("BALLSCATTER");
		message.setLocation(origin);
		message.setTarget(target);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendRerollChoice(int gameId, int playerId, int team, String teamName, String choice, int[][] tiles) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("REROLLCHOICE");
		message.setPlayer(playerId);
		message.setUserToChoose(team);
		message.setRerollChoice(choice);
		message.setTeamName(teamName);
		message.setLocation(tiles[0]);
		message.setTarget(tiles[1]);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendArmourRoll(int gameId, int playerId, String playerName, int armour, int[] rolls, String outcome) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("ARMOURROLL");
		message.setPlayer(playerId);
		message.setPlayerName(playerName);
		message.setRollNeeded(armour);
		message.setRollOutcome(outcome);
		List<Integer> rollList = new ArrayList<Integer>(Arrays.asList(new Integer[] { rolls[0], rolls[1] }));
		message.setRolled(rollList);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendInjuryRoll(int gameId, int playerId, String playerName, int[] rolls, String playerStatus,
			int[] location, String outcome) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("INJURYROLL");
		message.setRollOutcome(outcome);
		List<Integer> rollList = new ArrayList<Integer>(Arrays.asList(new Integer[] { rolls[0], rolls[1] }));
		message.setRolled(rollList);
		message.setPlayer(playerId);
		message.setPlayerName(playerName);
		message.setPlayerStatus(playerStatus);
		message.setLocation(location);
		;
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendTurnover(int gameId, int teamId, String teamName) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("TURNOVER");
		message.setTeamName(teamName);
		message.setUserToChoose(teamId);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendGameStatus(int gameId, int teamId, String teamName, TeamInGame team1, TeamInGame team2,
			int team1Score, int team2Score, int[] ballLocation, String phase) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("GAMESTATUS");
		message.setUserToChoose(teamId);
		message.setTeamName(teamName);
		message.setTeam1Name(team1.getName());
		message.setTeam2Name(team2.getName());
		message.setTeam1FullDetails(team1);
		message.setTeam2FullDetails(team2);
		message.setTeam1Score(team1Score);
		message.setTeam2Score(team2Score);
		message.setBallLocation(ballLocation);
		message.setPhase(phase);
		controller.sendMessageToBothUsers(gameId, message);
	}
	
	public void sendNewTurn(int gameId, int teamId, String teamName) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("NEWTURN");
		message.setUserToChoose(teamId);
		message.setTeamName(teamName);
		controller.sendMessageToBothUsers(gameId, message);
	}
	
	public void sendTouchdown(int gameId, int playerId, String playerName, int teamId,String teamName, int team1Score, int team2Score) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("TOUCHDOWN");
		message.setPlayer(playerId);
		message.setPlayerName(playerName);
		message.setUserToChoose(teamId);
		message.setTeamName(teamName);
		message.setTeam1Score(team1Score);
		message.setTeam2Score(team2Score);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendBlockInfo(int gameId, int player, int opponent, int[] location,int[] target, int[] block,
			int[][] attAssists, int[][] defAssists, int teamId) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("BLOCK");
		message.setNumberOfDice(block[0]);
		message.setUserToChoose(block[1]);
		message.setPlayer(player);
		message.setOpponent(opponent);
		message.setAttAssists(attAssists);
		message.setDefAssists(defAssists);
		message.setLocation(location);
		message.setTarget(target);
		controller.sendMessageToUser(gameId, teamId, message);
	}

// keeping this seperate rather than using route + block messages to reduce interference with other methods/ actions
public void sendBlitzDetails(int gameId, int player, int opponent, int[] blitzLocation, int[] blitzTarget, 
		                   int[][] attAssists, int[][]defAssists, List<jsonTile> route, int routeMACost, int[] block, int team) {
	MessageToClient message = new MessageToClient();
	message.setType("INFO");
	message.setAction("BLITZ");
	message.setNumberOfDice(block[0]);
	message.setUserToChoose(block[1]);
	message.setPlayer(player);
	message.setOpponent(opponent);
	message.setAttAssists(attAssists);
	message.setDefAssists(defAssists);
	message.setLocation(blitzLocation);
	message.setTarget(blitzTarget);
	message.setRouteMACost(routeMACost);
	message.setPlayer(player);
	message.setRoute(route);
	controller.sendMessageToUser(gameId, team, message);
}

	public void sendBlockDiceResult(int gameId, int player, String playerName, int opponent, String opponentName, int[] location, int[] target,
		List<Integer> rolled, int[][] attAssists, int[][] defAssists, List<String> rerollOptions, boolean reroll, int teamId) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("BLOCK");
		message.setRolled(rolled);
		message.setPlayer(player);
		message.setPlayerName(playerName);
		message.setOpponent(opponent);
		message.setOpponentName(opponentName);
		message.setAttAssists(attAssists);
		message.setDefAssists(defAssists);
		message.setLocation(location);
		message.setTarget(target);
		message.setRerollOptions(rerollOptions);
		message.setUserToChoose(teamId);
		message.setReroll(reroll);
		controller.sendMessageToBothUsers(gameId, message);
	}
    
	public void requestBlockDiceChoice(int gameId, int player, int opponent, int teamId) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("BLOCKDICECHOICE");
		message.setPlayer(player);
		message.setOpponent(opponent);
		controller.sendMessageToUser(gameId, teamId, message);	
	}

	public void sendBlockDiceChoice(int gameId, int player, int opponent, int diceChoice, String teamName, int teamId) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("BLOCKDICECHOICE");
		message.setPlayer(player);
		message.setOpponent(opponent);
		message.setDiceChoice(diceChoice);
		message.setUserToChoose(teamId);
		message.setTeamName(teamName);
		controller.sendMessageToBothUsers(gameId, message);	
	}

	public void sendSkillUsed(int gameId, int playerId, String playerName, int team, String details) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("SKILLUSED");
		message.setPlayer(playerId);
		message.setPlayerName(playerName);
		message.setUserToChoose(team); // team player belongs to
		message.setDescription(details);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendBlockSuccess(int gameId, int attacker, int defender, boolean blitz) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("BLOCKOVER");
		message.setPlayer(attacker);
		message.setOpponent(defender);
		message.setDescription((blitz == true ? "BLITZ" : "BLOCK"));
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void requestPushChoice(int gameId, int pusherId, int pushedId, int[] pusherLocation, int[] pushedLocation, List<jsonTile> pushOptions, int teamId) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("PUSHCHOICE");
		message.setPlayer(pusherId);
		message.setOpponent(pushedId);
		message.setLocation(pusherLocation);
		message.setTarget(pushedLocation);
		message.setSquares(pushOptions);
		message.setUserToChoose(teamId);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendPushResult(int gameId, int pushedId, String pushedName, int[] pushedFrom, int[] pushedTo, String type) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("PUSHRESULT");
		message.setPlayer(pushedId);
		message.setPlayerName(pushedName);
		message.setLocation(pushedFrom);
		message.setTarget(pushedTo);
		message.setDescription(type);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendBlitzUsed(int gameId, int team) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("TEAMBLITZED");
		message.setUserToChoose(team);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendThrowDetails(int gameId, Integer player, int[] location, int[] target, String targetName, int rollNeeded, int catchRoll, List<jsonTile> interceptLocations,
			int team) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("THROW");
		message.setPlayer(player);
		message.setLocation(location);
		message.setTarget(target);
		message.setOpponentName(targetName); // not really an opponent, but saves creating new field just for this
		message.setRollNeeded(rollNeeded);
		message.setSecondaryRollNeeded(catchRoll);
		message.setSquares(interceptLocations);
		controller.sendMessageToUser(gameId, team, message);
	}

	public void sendThrowRanges(int gameId, Integer playerId, int[] location, List<jsonTile> squares, int team) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("THROWRANGES");
		message.setPlayer(playerId);
		message.setLocation(location);
		message.setSquares(squares);
		controller.sendMessageToUser(gameId, team, message);
	}

	public void sendHandOffDetails(int gameId, int roll, Integer player, int[] location, int[] target, Integer opponent, int team) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("HANDOFF");
		message.setPlayer(player);
		message.setLocation(location);
		message.setTarget(target);
		message.setOpponent(opponent);
		message.setRollNeeded(roll);
		controller.sendMessageToUser(gameId, team, message);
	}

	public void sendHandOffAction(int gameId, int player, int[] location, String playerName, int[] target, int targetPlayer, String targetName) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("HANDOFF");
		message.setPlayer(player);
		message.setPlayerName(playerName);
		message.setLocation(location);
		message.setTarget(target);
		message.setOpponent(targetPlayer);
		message.setOpponentName(targetName);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendStandUpAction(int gameId, int player, String playerName, String end) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("STANDUP");
		message.setPlayer(player);
		message.setPlayerName(playerName);
		message.setEnd(end);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendInvalidMessage(int gameId, int team, String action, String description) {
		MessageToClient message = new MessageToClient();
		message.setType("INVALID");
		message.setAction(action);
		message.setDescription(description);
		controller.sendMessageToUser(gameId, team, message);
	}

	public void sendKickTarget(int gameId, int playerId, String playerName, int[] location, int[] target) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("KICKTARGET");
		message.setPlayer(playerId);
		message.setPlayerName(playerName);
		message.setLocation(location);
		message.setTarget(target);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendCoinTossWinner(int gameId, int teamId, String teamName) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("COINTOSS");
        message.setUserToChoose(teamId);
        message.setTeamName(teamName);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendWaitingForOpponent(int game, int team) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("WAITING");
		controller.sendMessageToUser(game, team, message);
	}

	public void sendKickOffChoice(int gameId, int team, String teamName, String choice) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("KICKOFFCHOICE");
		message.setUserToChoose(team);
		message.setTeamName(teamName);
		message.setDescription(choice);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendTouchBackRequest(int gameId, List<jsonTile> options, int team, String description) {
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("TOUCHBACKREQUEST");
		message.setUserToChoose(team);
		message.setSquares(options);
		message.setDescription(description);
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendTouchBackResult(int gameId, int playerId, String playerName) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("TOUCHBACKCHOICE");
		message.setPlayer(playerId);
		message.setPlayerName(playerName);
		controller.sendMessageToBothUsers(gameId, message);
	}

}
