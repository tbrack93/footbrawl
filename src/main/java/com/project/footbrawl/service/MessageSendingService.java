package com.project.footbrawl.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
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
	    controller.sendMessageToUser(gameId,  teamId, message);
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
	
	public void sendRollResult(int gameId, int playerId, String playerName, String rollType, int rollNeeded, List<Integer> rolled, String rollResult, int[] origin, int[]target, List<String> rerollOptions,
			                  int teamId) {
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
		controller.sendMessageToBothUsers(gameId, message);
	}

	public void sendRerollRequest(int gameId, int playerId, String type, List<String> options, int teamId, int otherTeam) {
		System.out.println(teamId);
		System.out.println(otherTeam);
		MessageToClient message = new MessageToClient();
		message.setType("ACTION");
		message.setAction("REROLL");
		message.setRollType(type);
		message.setPlayer(playerId);
		message.setRerollOptions(options);
		controller.sendMessageToUser(gameId, teamId, message);
		message.setType("INFO");
		message.setRerollOptions(null);
		controller.sendMessageToUser(gameId, otherTeam, message);
	}
	
}
