package com.project.footbrawl.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.footbrawl.service.GameLobbyService;

@Component
public class GameCleanUpTask {
	
	@Autowired
	private ApplicationContext context;
	
	@Scheduled(initialDelay = 10000, fixedRate = 900000)
    public void cleanUpGameServices(){
		GameLobbyService lobby = context.getBean(GameLobbyService.class);
		lobby.cleanUpGameServices();
	}
	
//	@Scheduled(fixedRate = 5000)
//	public void monitorMemory() {
//		System.out.println("My Size: " + ObjectSizeCalculator.getObjectSize(lobby));
//	}
	
}
