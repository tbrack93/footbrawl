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
				System.out.println("Decode MA Used: " + message.getRouteMACost());
				return;
			}
			if(action.equals("TEAMS")){
				lobby.getGameService(gameId).sendTeamsInfo(team);
				return;
			}
			if(action.equals("ROUTE")) {
				lobby.getGameService(gameId).sendRoute(message.getPlayer(), message.getTarget(), team);
			}
		}
	}
	
}
