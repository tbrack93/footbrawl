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
		if(type.equals("INFO")) {
			if(action.equals("MOVEMENT")){
				lobby.getGameService(gameId).showPossibleMovement(message.getPlayer(), message.getLocation(), message.getRouteMACost(), team);
				return;
			}
			if(action.equals("TEAMS")){
				lobby.getGameService(gameId).sendTeamsInfo(team);
				return;
			}
			if(action.equals("ROUTE")) {
				if(message.getWaypoints().size() != 0) {
					lobby.getGameService(gameId).sendWaypointRoute(message.getPlayer(), message.getTarget(), message.getWaypoints(), team);
				} else {
				lobby.getGameService(gameId).sendRoute(message.getPlayer(), message.getLocation(), message.getTarget(), team);
				}
			}
		}else if(type.equals("ACTION")) {
			if(action.equals("ROUTE")){
				lobby.getGameService(gameId).carryOutRouteAction(message.getPlayer(), message.getRoute(), team);
				return;
			}
		}
		
	}
	
}
