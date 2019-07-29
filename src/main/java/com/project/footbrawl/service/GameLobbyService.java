package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.footbrawl.entity.Game;
import com.project.footbrawl.entity.Player;
import com.project.footbrawl.entity.Skill;
import com.project.footbrawl.entity.Team;
import com.project.footbrawl.instance.PlayerInGame;

@Service
public class GameLobbyService {
	
	@Autowired
	private GameService gs;
		
	private Map<Integer, GameService> activeGames; // game id and gameservice
	
	public GameLobbyService(GameService gs) {
		this.gs = gs;
		activeGames = new HashMap<>();
		Player p = new Player();
		p.setName("Billy");
		p.setMA(10);
		p.setAG(3);
		p.setAV(6);
		p.setTeam(1);
		p.setST(8);
		Player p2 = new Player();
		p2.setName("Bobby");
		p2.setAG(2);
		p2.setMA(3);
		p2.setAV(6);
		p2.setTeam(2);
		p2.setST(3);
		Player p3 = new Player();
		p3.setName("Sam");
		p3.setMA(3);
		p3.setTeam(2);
		p3.setST(3);
		p3.setAV(2);
		p3.setAV(6);
		Skill block = new Skill("Block", "blocking is fun", "block");
		Skill dodge = new Skill("Dodge", "avoid your enemies", "dodge");
		List<Skill> skills = new ArrayList<>();
		skills.add(block);
		skills.add(dodge);
		p.setSkills(skills);
		Player p4 = new Player();
		p4.setName("Sarah");
		p4.setMA(10);
		p4.setAG(2);
		p4.setTeam(1);
		p4.setST(3);
		p4.setAV(6);
		p4.setSkills(skills);
		Player p5 = new Player();
		p5.setName("Bob");
		p5.setMA(4);
		p5.setAG(2);
		p5.setTeam(1);
		p5.setST(3);
		p.setImgUrl("/images/human_blitzer.png");
		p2.setImgUrl("/images/orc_lineman.png");
		p3.setImgUrl("/images/orc_lineman.png");
		p4.setImgUrl("/images/human_blitzer.png");
		p5.setImgUrl("/images/human_blitzer.png");
		Team team1 = new Team("bobcats");
		Team team2 = new Team("murderers");
		team1.addPlayer(p);
		team2.addPlayer(p2);
		team2.addPlayer(p3);
		team1.addPlayer(p4);
		team1.addPlayer(p5);
		team1.setTeamRerolls(4);
		team2.setTeamRerolls(4);
		Game g = new Game();
		g.setTeam1(team1);
		g.setTeam2(team2);
		g.setTeam1Score(1);
		g.setTeam2Score(0);
		gs.setGame(g);
		List<PlayerInGame> team1Players = gs.team1.getPlayersOnPitch();
		List<PlayerInGame> team2Players = gs.team2.getPlayersOnPitch();
		gs.pitch[6][5].addPlayer(team1Players.get(0));
		gs.pitch[7][5].addPlayer(team2Players.get(0));
		gs.pitch[7][7].addPlayer(team2Players.get(1));
		gs.pitch[8][5].addPlayer(team1Players.get(1));
		gs.pitch[20][12].addPlayer(team1Players.get(2));
		gs.pitch[21][12].addBall();
		gs.setActiveTeam(gs.team1);
		gs.team1.setTurn(4);
		gs.team2.setTurn(3);
		//team1Players.get(0).setStatus("prone");
		//team1Players.get(1).setStatus("stunned");
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
	
//	public static void main(String[] args) {
//		GameLobbyService test = new GameLobbyService();
//	}

}
