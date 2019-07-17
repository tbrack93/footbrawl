package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameLobbyService {
		
	private Map<Integer, GameService> activeGames; // game id and gameservice
	
	public GameLobbyService() {
		activeGames = new HashMap<>();
	}
	
	public GameService getGameService(int id) {
		return activeGames.get(id);
	}
	
	public void addGameService(GameService gs) {
		activeGames.put(gs.getGameId(), gs);
	}
	
	public void removeGameService(int id) {
		activeGames.remove(id);
	}
	
	public List<GameService> getActiveGames() {
		return new ArrayList<GameService>(activeGames.values());
	}

}
