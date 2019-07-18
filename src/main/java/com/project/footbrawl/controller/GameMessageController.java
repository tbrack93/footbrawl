package com.project.footbrawl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.project.footbrawl.instance.MessageFromClient;
import com.project.footbrawl.service.MessageDecoderService;

@Controller
public class GameMessageController {
	
	    @Autowired
	    private MessageDecoderService decoder;

		@MessageMapping("/game/gameplay/{game}/{team}")
		public void specificTeam(@DestinationVariable int game, @DestinationVariable int team, MessageFromClient message) throws Exception {
		  System.out.println(message);
		  decoder.decode(message, game);
		}
		
		@MessageMapping("/game/gameplay/{game}")
		public void bothTeams(@DestinationVariable String game) throws Exception {
		  
		}
		
		
		
		
}
