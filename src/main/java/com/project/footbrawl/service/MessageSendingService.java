package com.project.footbrawl.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import com.project.footbrawl.controller.GameMessageController;
import com.project.footbrawl.instance.MessageToClient;
import com.project.footbrawl.instance.jsonTile;

@Service
public class MessageSendingService {
	
	@Autowired
	GameMessageController controller;

	public MessageSendingService() {
		
	}

	public void sendMovementInfoMessage(int game, int team, int playerId, List<jsonTile> squares) {
		System.out.println("creating message");
		MessageToClient message = new MessageToClient();
		message.setType("INFO");
		message.setAction("MOVEMENT");
		message.setPlayer(playerId);
		message.setSquares(squares);
		controller.sendMessageToUser(game, team, message);
	}
	
}
