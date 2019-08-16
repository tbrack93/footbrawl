package com.project.footbrawl.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.footbrawl.service.GameLobbyService;

@Component
public class GameCleanUpTask {
	
	@Autowired
	private GameLobbyService lobby;
	
	@Scheduled(fixedRate = 900000)
    public void cleanUpGameServices(){
		lobby.cleanUpGameServices();
	}
	
//	@Scheduled(fixedRate = 5000)
//	public void monitorMemory() {
//		System.out.println("My Size: " + ObjectSizeCalculator.getObjectSize(lobby));
//	}
	
}
