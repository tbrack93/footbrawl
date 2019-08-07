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
		p.setMA(4);
		p.setAG(3);
		p.setAV(6);
		p.setTeam(1);
		p.setST(8);
		p.setType("Blitzer");
		Player p2 = new Player();
		p2.setName("Bobby");
		p2.setAG(2);
		p2.setMA(3);
		p2.setAV(6);
		p2.setTeam(2);
		p2.setST(3);
		p2.setType("Lineman");
		Player p3 = new Player();
		p3.setName("Sam");
		p3.setMA(3);
		p3.setTeam(2);
		p3.setST(3);
		p3.setAV(2);
		p3.setAV(6);
		p3.setType("Blitzer");
		Skill block = new Skill("Block", "blocking is fun", "block");
		Skill dodge = new Skill("Dodge", "avoid your enemies", "dodge");
		Skill sideStep = new Skill("Side Step", "Who do you think you're pushing?", "block");
		Skill catching = new Skill("Catch", "That ball is mine", "catch");
		Skill pass = new Skill("Pass", "fly my pretty", "throw");
		List<Skill> skills = new ArrayList<>();
		//skills.add(block);
		skills.add(dodge);
		skills.add(catching);
		skills.add(pass);
		//skills.add(sideStep);
		p.setSkills(skills);
		p3.setSkills(skills);
		skills.remove(0);
		skills.add(sideStep);
		//skills.add(block);
		p2.setSkills(skills);
		Player p4 = new Player();
		p4.setName("Sarah");
		p4.setMA(10);
		p4.setAG(2);
		p4.setTeam(1);
		p4.setST(3);
		p4.setAV(6);
		p4.setSkills(skills);
		p4.setType("Lineman");
		Player p5 = new Player();
		p5.setName("Bob");
		p5.setMA(10);
		p5.setAG(2);
		p5.setTeam(1);
		p5.setST(3);
		p5.setSkills(skills);
		p5.setType("Blitzer");
		Player p6 = new Player();
		p6.setName("Job");
		p6.setMA(3);
		p6.setAG(2);
		p6.setTeam(2);
		p6.setType("Blitzer");
		p6.setST(3);
		Player p7 = new Player();
		p7.setName("Kim");
		p7.setMA(3);
		p7.setAG(2);
		p7.setTeam(1);
		p7.setType("Blitzer");
		p7.setST(3);
		p7.setAV(10);
		Player p8 = new Player();
		p8.setName("Jim");
		p8.setMA(3);
		p8.setAG(2);
		p8.setTeam(1);
		p8.setType("Blitzer");
		p8.setST(3);
		p8.setAV(10);
		Player p9 = new Player();
		p9.setName("Lim");
		p9.setMA(3);
		p9.setAG(2);
		p9.setTeam(1);
		p9.setType("Blitzer");
		p9.setST(3);
		p9.setAV(10);
		Player p10 = new Player();
		p10.setName("Will");
		p10.setMA(3);
		p10.setAG(2);
		p10.setTeam(1);
		p10.setType("Blitzer");
		p10.setST(3);
		p10.setAV(10);
		Player p11 = new Player();
		p11.setName("Jill");
		p11.setMA(3);
		p11.setAG(2);
		p11.setTeam(1);
		p11.setType("Blitzer");
		p11.setST(3);
		p11.setAV(10);
		Player p12 = new Player();
		p12.setName("Simon");
		p12.setMA(3);
		p12.setAG(2);
		p12.setTeam(1);
		p12.setType("Blitzer");
		p12.setST(3);
		p12.setAV(10);
		Player p13 = new Player();
		p13.setName("Kimmy");
		p13.setMA(3);
		p13.setAG(2);
		p13.setTeam(1);
		p13.setType("Blitzer");
		p13.setST(3);
		p13.setAV(10);
		Player p14 = new Player();
		p14.setName("Tom");
		p14.setMA(3);
		p14.setAG(2);
		p14.setTeam(1);
		p14.setType("Blitzer");
		p14.setST(3);
		p14.setAV(10);
		p.setImgUrl("/images/human_blitzer.png");
		p2.setImgUrl("/images/orc_lineman.png");
		p3.setImgUrl("/images/orc_lineman.png");
		p4.setImgUrl("/images/human_blitzer.png");
		p5.setImgUrl("/images/human_blitzer.png");
		p6.setImgUrl("/images/human_blitzer.png");
		p7.setImgUrl("/images/human_blitzer.png");
		p8.setImgUrl("/images/human_blitzer.png");
		p9.setImgUrl("/images/human_blitzer.png");
		p10.setImgUrl("/images/human_blitzer.png");
		p11.setImgUrl("/images/human_blitzer.png");
		p12.setImgUrl("/images/human_blitzer.png");
		p13.setImgUrl("/images/human_blitzer.png");
		p14.setImgUrl("/images/human_blitzer.png");
		Team team1 = new Team("bobcats");
		Team team2 = new Team("murderers");
		team1.addPlayer(p);
		team2.addPlayer(p2);
		team2.addPlayer(p3);
		team1.addPlayer(p4);
		team1.addPlayer(p5);
		team2.addPlayer(p6);
		team1.addPlayer(p7);
		team1.addPlayer(p8);
		team1.addPlayer(p9);
		team1.addPlayer(p10);
		team1.addPlayer(p11);
		team1.addPlayer(p12);
		team1.addPlayer(p13);
		team1.addPlayer(p14);
	
		team1.setTeamRerolls(4);
		team2.setTeamRerolls(4);
		Game g = new Game();
		g.setTeam1(team1);
		g.setTeam2(team2);
		g.setTeam1Score(1);
		g.setTeam2Score(0);
		gs.setGame(g);
//		List<PlayerInGame> team1Players = gs.team1.getPlayersOnPitch();
//		List<PlayerInGame> team2Players = gs.team2.getPlayersOnPitch();
//		gs.pitch[5][2].addPlayer(team1Players.get(0));
//		gs.pitch[4][6].addPlayer(team2Players.get(0));
//		gs.pitch[5][5].addPlayer(team2Players.get(1));
//		gs.pitch[4][4].addPlayer(team1Players.get(1));
//		gs.pitch[4][5].addPlayer(team1Players.get(2));
//		gs.pitch[0][0].addPlayer(team2Players.get(2));
		//team1Players.get(2).setHasBall(true);
		//team2Players.get(0).setHasBall(true);
	 //   gs.pitch[4][3].addBall();
		gs.setActiveTeam(gs.team1);
		gs.team1.setTurn(4);
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team1.addToReserves(new PlayerInGame(p));
//		gs.team2.addToReserves(new PlayerInGame(p2));
		gs.team2.setTurn(3);
	//	team1Players.get(0).setStatus("prone");
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
