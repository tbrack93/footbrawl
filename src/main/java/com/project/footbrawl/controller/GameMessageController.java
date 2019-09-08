package com.project.footbrawl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import com.project.footbrawl.instance.MessageFromClient; 
import com.project.footbrawl.instance.MessageToClient;
import com.project.footbrawl.service.MessageDecoderService;

@Controller
public class GameMessageController {
	
	    @Autowired
	    private MessageDecoderService decoder;
	    
	    @Autowired	    
		private SimpMessageSendingOperations sending;
	    
		@MessageMapping("/game/gameplay/{game}/{team}")
		public void specificTeam(@DestinationVariable int game, @DestinationVariable int team, MessageFromClient message) throws Exception {
		  decoder.decode(message, game, team);
		}
		
		@MessageMapping("/game/gameplay/{game}")
		public void bothTeams(@DestinationVariable String game) throws Exception {
		  
		}
		
		public void sendMessageToUser(int game, int team, MessageToClient message) {
			sending.convertAndSend("/queue/game/" + game + "/" + team, message);
		}
		
		public void sendMessageToBothUsers(int game, MessageToClient message) {
			sending.convertAndSend("/topic/game/" + game, message);
		}
		
		
}
