package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import com.project.footbrawl.controller.GameMessageController;
import com.project.footbrawl.instance.MessageToClient;
import com.project.footbrawl.instance.PlayerInGame;
import com.project.footbrawl.instance.TeamInGame;
import com.project.footbrawl.instance.jsonTile;

@Service
public class MessageSendingService {

	@Autowired
	GameMessageController controller;

	public MessageSendingService() {

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

	public void sendTeamsInfo(int gameId, int teamId, TeamInGame team1, TeamInGame team2) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("TEAMS");
		message.setTeam1Name(team1.getName());
		message.setTeam2Name(team2.getName());
		message.setTeam1(team1.getPlayersOnPitch());
		message.setTeam2(team2.getPlayersOnPitch());
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
		message.setLocation(origin);
		message.setTarget(target);
		message.setUserToChoose(teamId);
		message.setRerollOptions(rerollOptions);
		message.setEnd(end);
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
			int team1Score, int team2Score, int[] ballLocation) {
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("NEWTURN");
		message.setUserToChoose(teamId);
		message.setTeamName(teamName);
		message.setTeam1Name(team1.getName());
		message.setTeam2Name(team2.getName());
		message.setTeam1FullDetails(team1);
		message.setTeam2FullDetails(team2);
		message.setTeam1Score(team1Score);
		message.setTeam2Score(team2Score);
		message.setBallLocation(ballLocation);
		controller.sendMessageToBothUsers(gameId, message);
	}
	

}
