package com.project.footbrawl.entity;

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
import javax.persistence.Table;

@Entity
@Table(name="player")
public class Player {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private int id;
	
	@Column(name="name")
	private String name;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade= {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name="playertype_id")
	private PlayerArchetype type;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade= {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name="team_id")
	private Team teamId;
	
	public Player() {
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

	public int getMA() {
		return type.getMA();
	}

	public int getST() {
		return type.getST();
	}

	public int getAG() {
		return type.getAG();
	}

	public int getAV() {
		return type.getAV();
	}

	public int getCost() {
		return type.getCost();
	}

	public List<Skill> getSkills() {
		return type.getSkills();
	}

	public int getTeam() {
		return teamId.getId();
	}
	
	public String getImgUrl() {
		return type.getImgUrl();
	}
	
	public boolean hasSkill(String name) {
		for(Skill s : this.getSkills()) {
			if(s.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return Integer.toString(id);
	}

	public PlayerArchetype getArchetype() {
		return type;
	}
	
	public String getType() {
		return type.getType();
	}

}
