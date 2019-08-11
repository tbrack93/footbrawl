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
				lobby.getGameService(gameId).showPossibleActions(message.getPlayer(), team);
			} else if (action.equals("MOVEMENT")) {
				lobby.getGameService(gameId).showPossibleMovement(message.getPlayer(), message.getLocation(),
						message.getRouteMACost(), team);
			} else if (action.equals("TEAMS")) {
				lobby.getGameService(gameId).sendTeamsInfo(team);
			} else if (action.equals("ROUTE")) {
				if (message.getWaypoints().size() != 0) {
					lobby.getGameService(gameId).sendWaypointRoute(message.getPlayer(), message.getTarget(),
							message.getWaypoints(), team);
				} else {
					lobby.getGameService(gameId).sendRoute(message.getPlayer(), message.getLocation(),
							message.getTarget(), team);
				}
				return;
			} else if (action.equals("BLOCK")) {
				lobby.getGameService(gameId).sendBlockDetails(message.getPlayer(), message.getOpponent(),
						message.getLocation(), team);
			} else if (action.equals("BLITZ")) {
				lobby.getGameService(gameId).sendBlitzDetails(message.getPlayer(), message.getOpponent(),
						message.getWaypoints(), message.getTarget(), team);
			} else if (action.equals("THROWRANGES")) {
				lobby.getGameService(gameId).sendThrowRange(message.getPlayer(), message.getLocation(), team);
			} else if (action.equals("THROW")) {
				lobby.getGameService(gameId).sendThrowDetails(message.getPlayer(), message.getTarget(), team);
			} else if (action.equals("HANDOFF")) {
				lobby.getGameService(gameId).sendHandOffDetails(message.getPlayer(), message.getTarget(),
						message.getOpponent(), team);
			}
		} else if (type.equals("ACTION")) {
			if (action.equals("ROUTE")) {
				lobby.getGameService(gameId).carryOutRouteAction(message.getPlayer(), message.getRoute(), team);
				return;
			} else if (action.equals("REROLL")) {
				lobby.getGameService(gameId).carryOutReroll(message.getPlayer(), team, message.getRerollChoice());
			} else if (action.equals("ENDTURN")) {
				lobby.getGameService(gameId).endTurn(team);
			} else if (action.equals("BLOCK")) {
				lobby.getGameService(gameId).carryOutBlock(message.getPlayer(), message.getOpponent(),
						message.getLocation(), message.isFollowUp(), false, team);
			} else if (action.equals("BLITZ")) {
				lobby.getGameService(gameId).carryOutBlitz(message.getPlayer(), message.getOpponent(),
						message.getRoute(), message.getTarget(), message.isFollowUp(), team);
			} else if (action.equals("BLOCKDICECHOICE")) {
				lobby.getGameService(gameId).carryOutBlockChoice(message.getDiceChoice(), message.getPlayer(),
						message.getOpponent(), message.isFollowUp(), team);
			} else if (action.equals("PUSHCHOICE")) {
				lobby.getGameService(gameId).carryOutPushChoice(message.getTarget());
			} else if (action.equals("THROW")) {
				lobby.getGameService(gameId).carryOutThrow(message.getPlayer(), message.getLocation(),
						message.getTarget(), team);
			} else if (action.equals("HANDOFF")) {
				lobby.getGameService(gameId).carryOutHandOff(message.getPlayer(), message.getTarget(),
						message.getOpponent(), team);
			} else if (action.equals("STANDUP")) {
				lobby.getGameService(gameId).carryOutStandUp(message.getPlayer());
			} else if (action.equals("PLACEMENT")) {
				lobby.getGameService(gameId).carryOutPlacement(message.getPlayer(), message.getTarget());
			} else if (action.equals("BENCH")) {
				lobby.getGameService(gameId).benchPlayer(message.getPlayer());
			} else if (action.equals("ENDSETUP")) {
				lobby.getGameService(gameId).endSetup(team);
			} else if (action.equals("KICK")) {
				lobby.getGameService(gameId).kickBall(message.getTarget());
			} else if (action.equals("KICKCHOICE")) {
				lobby.getGameService(gameId).chooseKickOff(team, message.getDescription());
			} else if (action.equals("JOINGAME")) {
				lobby.getGameService(gameId).joinGame(team);
			} else if (action.equals("TOUCHBACKCHOICE")){
				lobby.getGameService(gameId).actOnTouchBack(message.getPlayer(), team);
			}
		}

	}

}
