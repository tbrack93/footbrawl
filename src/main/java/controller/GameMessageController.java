package controller;

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

		@MessageMapping("/game/{game}/player")
		public void message(@DestinationVariable String game) throws Exception {
		  
		 }
}
