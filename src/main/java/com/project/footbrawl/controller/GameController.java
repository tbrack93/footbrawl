package com.project.footbrawl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/game")
public class GameController {
	
	@GetMapping("/test")
	public void testing() {
		System.out.println("test");
	}
	
	@GetMapping("/gameplay/{game}/{team}")
	public String startGamePlay(@PathVariable("game") int gameId, @PathVariable("team") int teamId, Model theModel) {
		theModel.addAttribute("gameId", gameId);
		theModel.addAttribute("teamId", teamId);
		return "gameplay";
	}
	
}
