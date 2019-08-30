package com.project.footbrawl.service;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.project.footbrawl.DAO.GameRepository;
import com.project.footbrawl.DAO.TeamRepository;
import com.project.footbrawl.entity.Game;

@Service
@Scope("singleton")
public class GameLobbyService {

	@Autowired
	private AutowireCapableBeanFactory beanFactory;
	
	private GameRepository gameRepo;
	private TeamRepository teamRepo;

	private Game defaultGame;

	private Map<Integer, GamePlayService> activeGames; // game id and gameservice

	public GameLobbyService(AutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.gameRepo = beanFactory.getBean(GameRepository.class);
		this.teamRepo = beanFactory.getBean(TeamRepository.class);
		if (defaultGame == null) {
			activeGames = new HashMap<>();
			Game g = new Game();
			g.setTeam1(teamRepo.findById(1).get());
			g.setTeam2(teamRepo.findById(2).get());
			gameRepo.save(g);
			defaultGame = g;
			GamePlayService gs = new GamePlayService();
			beanFactory.autowireBean(gs);
			gs.setGame(g);
			//gs.setPhase("ended");
			addGamePlayService(gs);
			//System.out.println("My Size: " + ObjectSizeCalculator.getObjectSize(this));
		}
	}

	public GamePlayService getGamePlayService(int id) {
		return activeGames.get(id);
	}

	public void addGamePlayService(GamePlayService gs) {
		activeGames.put(gs.getGameId(), gs);
	}

	public void removeGamePlayService(int id) {
		activeGames.remove(id);
	}

	public List<GamePlayService> getActiveGames() {
		return new ArrayList<GamePlayService>(activeGames.values());
	}

	public void createNewGameAndService() {
		GamePlayService gs = new GamePlayService();
		beanFactory.autowireBean(gs);
		Game g = new Game();
		g.duplicateGame(defaultGame);
		gameRepo.save(g);
		gs.setGame(g);
		activeGames.put(g.getId(), gs);
	}

	public synchronized int[] assignToGame(String teamType, boolean invite) {
		int teamId = 0;
		if(teamType.contains("orcs")) {
			teamId = 2;
		} else if(teamType.contains("humans")) {
			teamId = 1;
		}
		int game;
		int team;
		ArrayList<Integer> active = new ArrayList<>(activeGames.keySet());
		for (Integer i : active) {
			GamePlayService gs = activeGames.get(i);

			if (gs.isWaitingForPlayers() == true) {
				if(invite == false && (teamId == 0 || teamId == 1 && !gs.isTeam1Assigned() || teamId == 2 && !gs.isTeam2Assigned())) {
				  game = gs.getGameId();
				  team = gs.assignPlayer(teamId);
				  return new int[] { game, team, 0};
				}
				if(invite == true && !gs.isTeam1Assigned() && !gs.isTeam2Assigned()) {
				  game = gs.getGameId();
				  team = gs.assignPlayer(teamId);
				  int team2 = gs.assignPlayer(0);
				  return new int[] { game, team, team2 };
				}
			}
		}
		createNewGameAndService();
//     	System.out.println("My Size: " + ObjectSizeCalculator.getObjectSize(this));
		return assignToGame(teamType, invite);
	}

	public void cleanUpGameServices() {
		Iterator<Map.Entry<Integer, GamePlayService>> games = activeGames.entrySet().iterator();
		while(games.hasNext()) {
			Map.Entry<Integer, GamePlayService> game = games.next();
			GamePlayService gs = game.getValue();
			if(gs.isGameFinished() || new Date().getTime() - gs.getCreated().getTime() > 9000000) {
				//System.out.println("Removing game: " + gs.getGameId());
				gs = null; 
				games.remove();
			}
		}
	}

//	public static void main(String[] args) {
//		GameLobbyService test = new GameLobbyService();
//	}

}
