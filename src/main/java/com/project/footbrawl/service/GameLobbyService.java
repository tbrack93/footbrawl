package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.footbrawl.entity.Game;
import com.project.footbrawl.entity.Player;
import com.project.footbrawl.entity.Skill;
import com.project.footbrawl.entity.Team;
import com.project.footbrawl.instance.PlayerInGame;

@Service
public class GameLobbyService {
		
	private Map<Integer, GameService> activeGames; // game id and gameservice
	
	public GameLobbyService() {
		activeGames = new HashMap<>();
		Player p = new Player();
		p.setName("Billy");
		p.setMA(4);
		p.setAG(6);
		p.setTeam(1);
		p.setST(9);
		Player p2 = new Player();
		p2.setName("Bobby");
		p2.setAG(10);
		p2.setMA(3);
		p2.setTeam(2);
		p2.setST(3);
		Player p3 = new Player();
		p3.setName("Sam");
		p3.setMA(3);
		p3.setTeam(2);
		p3.setST(3);
		p3.setAV(2);
		Skill block = new Skill("Block", "blocking is fun", "block");
		List<Skill> skills = new ArrayList<>();
		skills.add(block);
		p3.setSkills(skills);
		Player p4 = new Player();
		p4.setName("Sarah");
		p4.setMA(3);
		p4.setAG(4);
		p4.setTeam(1);
		p4.setST(3);
		Team team1 = new Team("bobcats");
		Team team2 = new Team("murderers");
		team1.addPlayer(p);
		team2.addPlayer(p2);
		team2.addPlayer(p3);
		team1.addPlayer(p4);
		Game g = new Game();
		g.setTeam1(team1);
		g.setTeam2(team2);
		GameService gs = new GameService(g);
		List<PlayerInGame> team1Players = gs.team1.getPlayersOnPitch();
		List<PlayerInGame> team2Players = gs.team2.getPlayersOnPitch();
		gs.pitch[0][1].addPlayer(team1Players.get(0));
		gs.pitch[5][3].addPlayer(team1Players.get(1));
		gs.pitch[7][5].addPlayer(team2Players.get(0));
		gs.pitch[7][7].addPlayer(team2Players.get(1));
		gs.setActiveTeam(gs.team1);
		gs.activePlayer = team1Players.get(0);
		team1Players.get(0).setActedThisTurn(false);
		team1Players.get(0).setActionOver(false);
		activeGames.put(gs.getGameId(), gs);
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
