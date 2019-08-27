package com.project.footbrawl.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="teamtype")
public class TeamType {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private int id;
	
	@Column(name="name")
	private String name;
	
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
	@JoinTable(name="teamtype_playertype",
	           joinColumns=@JoinColumn(name="teamtype_id"),
	           inverseJoinColumns=@JoinColumn(name="playertype_id"))
	private List<PlayerArchetype> allowedPlayerTypes;
	
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
	
	public List<PlayerArchetype> getAllowedPlayerTypes() {
		return allowedPlayerTypes;
	}
	
	public void setAllowedPlayerTypes(List<PlayerArchetype> allowedPlayerTypes) {
		this.allowedPlayerTypes = allowedPlayerTypes;
	}
	
	public void addAllowedPlayerType(PlayerArchetype type) {
		allowedPlayerTypes.add(type);
	}

}
