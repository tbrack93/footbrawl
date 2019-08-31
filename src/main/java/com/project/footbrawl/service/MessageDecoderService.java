package com.project.footbrawl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.footbrawl.instance.MessageFromClient;

@Service
public class MessageDecoderService {

	@Autowired
	GameLobbyService lobby;

	public MessageDecoderService() {
	}

	public void decode(MessageFromClient message, int gameId, int team) {
		String type = message.getType();
		String action = message.getAction();
		if (type.equals("INFO")) {
			if (action.equals("ACTIONS")) {
				lobby.getGamePlayService(gameId).showPossibleActions(message.getPlayer(), team);
			} else if (action.equals("MOVEMENT")) {
				lobby.getGamePlayService(gameId).showPossibleMovement(message.getPlayer(), message.getLocation(),
						message.getRouteMACost(), team);
			} else if (action.equals("TEAMS")) {
				lobby.getGamePlayService(gameId).sendTeamsInfo(team);
			} else if (action.equals("ROUTE")) {
				if (message.getWaypoints().size() != 0) {
					lobby.getGamePlayService(gameId).sendWaypointRoute(message.getPlayer(), message.getTarget(),
							message.getWaypoints(), team);
				} else {
					lobby.getGamePlayService(gameId).sendRoute(message.getPlayer(), message.getLocation(),
							message.getTarget(), team);
				}
				return;
			} else if (action.equals("BLOCK")) {
				lobby.getGamePlayService(gameId).sendBlockDetails(message.getPlayer(), message.getOpponent(),
						message.getLocation(), team);
			} else if (action.equals("BLITZ")) {
				lobby.getGamePlayService(gameId).sendBlitzDetails(message.getPlayer(), message.getOpponent(),
						message.getWaypoints(), message.getTarget(), team);
			} else if (action.equals("THROWRANGES")) {
				lobby.getGamePlayService(gameId).sendThrowRange(message.getPlayer(), message.getLocation(), team);
			} else if (action.equals("THROW")) {
				lobby.getGamePlayService(gameId).sendThrowDetails(message.getPlayer(), message.getTarget(), team);
			} else if (action.equals("HANDOFF")) {
				lobby.getGamePlayService(gameId).sendHandOffDetails(message.getPlayer(), message.getTarget(),
						message.getOpponent(), team);
			}
		} else if (type.equals("ACTION")) {
			if (action.equals("ROUTE")) {
				lobby.getGamePlayService(gameId).carryOutRouteAction(message.getPlayer(), message.getRoute(), team);
				return;
			} else if (action.equals("REROLL")) {
				lobby.getGamePlayService(gameId).carryOutReroll(message.getPlayer(), team, message.getRerollChoice());
			} else if (action.equals("ENDTURN")) {
				lobby.getGamePlayService(gameId).endTurn(team);
			} else if (action.equals("BLOCK")) {
				lobby.getGamePlayService(gameId).carryOutBlock(message.getPlayer(), message.getOpponent(),
						message.getLocation(), false, team);
			} else if (action.equals("BLITZ")) {
				lobby.getGamePlayService(gameId).carryOutBlitz(message.getPlayer(), message.getOpponent(),
						message.getRoute(), message.getTarget(), team);
			} else if (action.equals("BLOCKDICECHOICE")) {
				lobby.getGamePlayService(gameId).carryOutBlockChoice(message.getDiceChoice(), message.getPlayer(),
						message.getOpponent(), team);
			} else if (action.equals("PUSHCHOICE")) {
				lobby.getGamePlayService(gameId).carryOutPushChoice(message.getTarget());
			} else if (action.equals("THROW")) {
				lobby.getGamePlayService(gameId).carryOutThrow(message.getPlayer(), message.getLocation(),
						message.getTarget(), team);
			} else if (action.equals("HANDOFF")) {
				lobby.getGamePlayService(gameId).carryOutHandOff(message.getPlayer(), message.getTarget(),
						message.getOpponent(), team);
			} else if (action.equals("STANDUP")) {
				lobby.getGamePlayService(gameId).carryOutStandUp(message.getPlayer());
			} else if (action.equals("PLACEMENT")) {
				lobby.getGamePlayService(gameId).carryOutPlacement(message.getPlayer(), message.getTarget());
			} else if (action.equals("BENCH")) {
				lobby.getGamePlayService(gameId).benchPlayer(message.getPlayer());
			} else if (action.equals("ENDSETUP")) {
				lobby.getGamePlayService(gameId).endSetup(team);
			} else if (action.equals("KICK")) {
				lobby.getGamePlayService(gameId).kickBall(message.getTarget());
			} else if (action.equals("KICKCHOICE")) {
				lobby.getGamePlayService(gameId).chooseKickOff(team, message.getDescription());
			} else if (action.equals("JOINGAME")) {
				lobby.getGamePlayService(gameId).joinGame(team);
			} else if (action.equals("TOUCHBACKCHOICE")){
				lobby.getGamePlayService(gameId).actOnTouchBack(message.getPlayer(), team);
			} else if(action.equals("RESETGAME")){
				lobby.getGamePlayService(gameId).resetGame();
			} else if(action.equals("INTERCEPTCHOICE")) {
				lobby.getGamePlayService(gameId).actOnIntercept(team, message.getPlayer(), message.getLocation());
			} else if(action.equals("AUTOSETUP")) {
				lobby.getGamePlayService(gameId).autoSetupTeam(message.getDescription(), team);
			} else if(action.equals("FOLLOWUPCHOICE")){
				lobby.getGamePlayService(gameId).followUpChoice(message.isFollowUp());
			}
		}

	}

}
