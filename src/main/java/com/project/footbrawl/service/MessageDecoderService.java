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
	
	public void decode(MessageFromClient message, int gameId) {
		String type = message.getType();
		if(type.equals("INFO")) {
			if(message.getAction().equals("MOVEMENT")){
				lobby.getGameService(gameId).showPossibleMovement(message.getPlayer(), message.getLocation());
			}
		}
	}
	
}
