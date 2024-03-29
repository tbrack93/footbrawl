package com.project.footbrawl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.footbrawl.service.GameLobbyService;

@Controller
@RequestMapping("/game")
public class GameController {
	
	@Autowired
	private ApplicationContext context;
	
	@GetMapping(path = "/join")
	public String joinGame(@RequestParam("team") String team, @RequestParam("invite") boolean invite) {
		GameLobbyService lobby = (GameLobbyService) context.getBean(GameLobbyService.class);
		int[] target = lobby.assignToGame(team, invite);
		System.out.println("in join");
		return "redirect:/game/gameplay/" + target[0] + "/" + target[1] + "?invite=" + target[2];
	}
	
	@GetMapping("/test")
	public void testing() {
		System.out.println("test");
	}
	
	@GetMapping("/gameplay/{game}/{team}")
	public String startGamePlay(@PathVariable("game") int gameId, @PathVariable("team") int teamId, @RequestParam("invite") int invited, Model theModel) {
		theModel.addAttribute("gameId", gameId);
		theModel.addAttribute("teamId", teamId);
		theModel.addAttribute("invitedTeam", invited);
		return "gameplay";
	}
	
}
