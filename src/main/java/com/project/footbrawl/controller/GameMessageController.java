package com.project.footbrawl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class GameMessageController {
		
		@Autowired	    
		private SimpMessageSendingOperations messagingTemplate;

		@MessageMapping("/game/gameplay/{game}/{team}")
		public void specificTeam(@DestinationVariable int game, @DestinationVariable int team) throws Exception {
		  
		}
		
		@MessageMapping("/game/gameplay/{game}")
		public void bothTeams(@DestinationVariable String game) throws Exception {
		  
		}
		
		
		
		
}
