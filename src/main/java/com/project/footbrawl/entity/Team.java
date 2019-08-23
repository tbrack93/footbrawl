package com.project.footbrawl.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="team")
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private int id;
	
	@Column(name="name")
	private String name;
	
	@ManyToOne(cascade= {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name="teamtype_id")
	private TeamType type;
	
	@Column(name="rerolls")
	private int teamRerolls;
	
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, 
			   mappedBy="teamId", orphanRemoval = true)
	private List<Player> players;


	public Team() {
		players = new ArrayList<Player>();
	}

	public Team(String name) {
		players = new ArrayList<Player>();
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public void setPlayers(List<Player> players) {
		this.players = players;
	}

	public Player getPlayer(int id) {
		for (Player p : players) {
			if (p.getId() == id) {
				return p;
			}
		}
		return null;
	}

	public void addPlayer(Player p) {
		players.add(p);
	}

	public int getTeamRerolls() {
		return teamRerolls;
	}

	public void setTeamRerolls(int teamRerolls) {
		this.teamRerolls = teamRerolls;
	}

	public TeamType getType() {
		return type;
	}

	public void setType(TeamType type) {
		this.type = type;
	}
	
	

}
