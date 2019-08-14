package com.project.footbrawl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import com.project.footbrawl.entity.Game;
import com.project.footbrawl.entity.Player;
import com.project.footbrawl.entity.Skill;
import com.project.footbrawl.entity.Team;

@Service
public class GameLobbyService {

    
    @Autowired 
    private AutowireCapableBeanFactory beanFactory;
	
	private GameService gs;
	
	private GameService gs2;
	
	private GameService gs3;
	
	private Game defaultGame;
		
	private Map<Integer, GameService> activeGames; // game id and gameservice
	
	public GameLobbyService(AutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		GameService gs = new GameService();
		beanFactory.autowireBean(gs);
//		GameService gs2 = factory.getObject();
//		GameService gs3 = factory.getObject();
		System.out.println(gs);
		Skill block = new Skill("Block", "blocking is fun", "block");
		Skill dodge = new Skill("Dodge", "avoid your enemies", "dodge");
		Skill sideStep = new Skill("Side Step", "Who do you think you're pushing?", "block");
		Skill catching = new Skill("Catch", "That ball is mine", "catch");
		Skill pass = new Skill("Pass", "fly my pretty", "throw");
		Skill sureHands = new Skill("Sure Hands", "reliable mitts", "ball");
		activeGames = new HashMap<>();
		Player p = new Player();
		p.setName("Billy");
		p.setMA(7);
		p.setAG(3);
		p.setAV(8);
		p.setTeam(1);
		p.setST(3);
		p.setType("Blitzer");
		p.setImgUrl("/images/human_blitzer.png");
		p.addSkill(block);
		Player p2 = new Player();
		p2.setName("Bobby");
		p2.setAG(3);
		p2.setMA(5);
		p2.setAV(9);
		p2.setTeam(2);
		p2.setST(3);
		p2.setType("Lineman");
		p2.setImgUrl("/images/orc_lineman.png");
		Player p3 = new Player();
		p3.setName("Sam");
		p3.setMA(5);
		p3.setTeam(2);
		p3.setST(3);
		p3.setAV(8);
		p3.setAG(3);
		p3.setImgUrl("/images/orc_thrower.png");
		p3.setType("Thrower");
		p3.addSkill(pass);
		p3.addSkill(sureHands);
		p3.addSkill(sideStep);
		
		//skills.add(block);
//		skills.add(dodge);
//		skills.add(catching);
//		skills.add(pass);
		//skills.add(sideStep);
		//skills.add(block);
		Player p4 = new Player();
		p4.setName("Sarah");
		p4.setMA(7);
		p4.setAG(3);
		p4.setTeam(1);
		p4.setST(3);
		p4.setAV(8);
		p4.setImgUrl("/images/human_blitzer.png");
		p4.addSkill(block);
		//p4.setSkills(skills);
		p4.setType("Blitzer");
		Player p5 = new Player();
		p5.setName("Bob");
		p5.setMA(8);
		p5.setAG(3);
		p5.setTeam(1);
		p5.setST(2);
		p5.setAV(7);
		//p5.setSkills(skills);
		p5.setType("Catcher");
		p5.addSkill(catching);
		p5.addSkill(dodge);
		p5.addSkill(sideStep);
		p5.setImgUrl("/images/human_catcher.png");
		Player p6 = new Player();
		p6.setName("Job");
		p6.setMA(6);
		p6.setAG(3);
		p6.setTeam(2);
		p6.setType("Blitzer");
		p6.setST(3);
		p6.setAV(9);
		p6.setImgUrl("/images/orc_blitzer.png");
		p6.addSkill(block);
		Player p7 = new Player();
		p7.setName("Kim");
		p7.setMA(6);
		p7.setAG(3);
		p7.setTeam(1);
		p7.setType("Thrower");
		p7.setST(3);
		p7.setAV(8);
		p7.setImgUrl("/images/human_thrower.png");
		p7.addSkill(pass);
		p7.addSkill(sureHands);
		Player p8 = new Player();
		p8.setName("Jim");
		p8.setMA(6);
		p8.setAG(3);
		p8.setTeam(1);
		p8.setType("Lineman");
		p8.setST(3);
		p8.setAV(8);
		p8.setImgUrl("/images/human_lineman.png");
		Player p9 = new Player();
		p9.setName("Lim");
		p9.setMA(5);
		p9.setAG(2);
		p9.setTeam(1);
		p9.setType("Ogre");
		p9.setST(5);
		p9.setAV(9);
		p9.setImgUrl("/images/ogre.png"); 
		Player p10 = new Player();
		p10.setName("Will");
		p10.setMA(7);
		p10.setAG(3);
		p10.setTeam(1);
		p10.setType("Blitzer");
		p10.setST(3);
		p10.setAV(8);
		p10.setImgUrl("/images/human_blitzer.png");
		p10.addSkill(block);
		Player p11 = new Player();
		p11.setName("Jill");
		p11.setMA(6);
		p11.setAG(3);
		p11.setTeam(1);
		p11.setType("Lineman");
		p11.setST(3);
		p11.setAV(8);
		p11.setImgUrl("/images/human_lineman.png");
		Player p12 = new Player();
		p12.setName("Simon");
		p12.setMA(8);
		p12.setAG(3);
		p12.setTeam(1);
		p12.setType("Catcher");
		p12.setST(2);
		p12.setAV(10);
		p12.setImgUrl("/images/human_catcher.png");
		p12.addSkill(catching);
		p12.addSkill(dodge);
		p12.addSkill(sideStep);
		Player p13 = new Player();
		p13.setName("Kimmy");
		p13.setMA(6);
		p13.setAG(3);
		p13.setTeam(1);
		p13.setType("Lineman");
		p13.setST(3);
		p13.setAV(8);
		p13.setImgUrl("/images/human_lineman.png");
		Player p14 = new Player();
		p14.setName("Tom");
		p14.setMA(6);
		p14.setAG(3);
		p14.setTeam(1);
		p14.setType("Lineman");
		p14.setST(3);
		p14.setAV(8);
		p14.setImgUrl("/images/human_lineman.png");
		Player p15 = new Player();
		p15.setName("Darryl");
		p15.setMA(6);
		p15.setAG(3);
		p15.setTeam(2);
		p15.setType("Blitzer");
		p15.setST(3);
		p15.setAV(9);
		p15.setImgUrl("/images/orc_blitzer.png");
		p15.addSkill(block);
		Player p16 = new Player();
		p16.setName("Keith");
		p16.setMA(5);
		p16.setTeam(2);
		p16.setST(3);
		p16.setAV(8);
		p16.setAG(3);
		p16.setImgUrl("/images/orc_thrower.png");
		p16.setType("Thrower");
		p16.addSkill(pass);
		p16.addSkill(sureHands);
		p16.addSkill(sideStep);
		Player p17 = new Player();
		p17.setName("Colby");
		p17.setAG(3);
		p17.setMA(5);
		p17.setAV(9);
		p17.setTeam(2);
		p17.setST(3);
		p17.setType("Lineman");
		p17.setImgUrl("/images/orc_lineman.png");
		Player p18 = new Player();
		p18.setName("David");
		p18.setAG(2);
		p18.setMA(4);
		p18.setAV(9);
		p18.setTeam(2);
		p18.setST(4);
		p18.setType("Black Orc");
		p18.setImgUrl("/images/black_orc.png");
		Player p19 = new Player();
		p19.setName("Chris");
		p19.setAG(2);
		p19.setMA(4);
		p19.setAV(9);
		p19.setTeam(2);
		p19.setST(4);
		p19.setType("Black Orc");
		p19.setImgUrl("/images/black_orc.png");
		Player p20 = new Player();
		p20.setName("Ifson");
		p20.setAG(1);
		p20.setMA(4);
		p20.setAV(9);
		p20.setTeam(2);
		p20.setST(5);
		p20.setType("Troll");
		p20.setImgUrl("/images/troll.png");
		Player p21 = new Player();
		p21.setName("Michael");
		p21.setAG(3);
		p21.setMA(5);
		p21.setAV(9);
		p21.setTeam(2);
		p21.setST(3);
		p21.setType("Lineman");
		p21.setImgUrl("/images/orc_lineman.png");
		Player p22 = new Player();
		p22.setName("Tony");
		p22.setAG(3);
		p22.setMA(6);
		p22.setAV(7);
		p22.setTeam(2);
		p22.setST(2);
		p22.setType("Goblin");
		p22.setImgUrl("/images/goblin.png");
		p22.addSkill(dodge);
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
		team2.addPlayer(p15);
		team2.addPlayer(p16);
		team2.addPlayer(p17);
		team2.addPlayer(p18);
		team2.addPlayer(p19);
		team2.addPlayer(p20);
		team2.addPlayer(p21);
		team2.addPlayer(p22);
	
		team1.setTeamRerolls(4);
		team2.setTeamRerolls(4);
		Game g = new Game();
		g.setTeam1(team1);
		g.setTeam2(team2);
		defaultGame = g;
	//	g.setTeam1Score(1);
	//	g.setTeam2Score(0);
		gs.setGame(g);
//		Game g2 = new Game();
//		g2.setTeam1(team1);
//		g2.setTeam2(team2);
//		gs2.setGame(g2);
//		Game g3 = new Game();
//		g3.setTeam1(team1);
//		g3.setTeam2(team2);
//		gs3.setGame(g3);
////		List<PlayerInGame> team1Players = gs.team1.getPlayersOnPitch();
////		List<PlayerInGame> team2Players = gs.team2.getPlayersOnPitch();
////		gs.pitch[5][2].addPlayer(team1Players.get(0));
////		gs.pitch[4][6].addPlayer(team2Players.get(0));
////		gs.pitch[5][5].addPlayer(team2Players.get(1));
////		gs.pitch[4][4].addPlayer(team1Players.get(1));
////		gs.pitch[4][5].addPlayer(team1Players.get(2));
////		gs.pitch[0][0].addPlayer(team2Players.get(2));
//		//team1Players.get(2).setHasBall(true);
//		//team2Players.get(0).setHasBall(true);
//	 //   gs.pitch[4][3].addBall();
//	//	gs.setActiveTeam(gs.team2);
//		//gs.team1.setTurn(4);
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team1.addToReserves(new PlayerInGame(p));
////		gs.team2.addToReserves(new PlayerInGame(p2));
//		//gs.team2.setTurn(3);
//	//	team1Players.get(0).setStatus("prone");
//		//team1Players.get(1).setStatus("stunned");
		activeGames.put(gs.getGameId(), gs);
//		activeGames.put(gs2.getGameId(), gs2);
//		activeGames.put(gs3.getGameId(), gs3);
		createNewGameAndService();
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
	
	public void createNewGameAndService() {
		GameService gs = new GameService();
		beanFactory.autowireBean(gs);
		Game g = new Game();
		g.duplicateGame(defaultGame);
		gs.setGame(g);
		activeGames.put(g.getId(), gs);
	}
	
//	public static void main(String[] args) {
//		GameLobbyService test = new GameLobbyService();
//	}

}
