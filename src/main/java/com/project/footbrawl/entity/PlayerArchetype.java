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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name="playertype")
public class PlayerArchetype {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private int id;
	
	@Column(name="type")
	private String type;
	
	@Column(name="ma")
	private int MA;
	
	@Column(name="st")
	private int ST;
	
	@Column(name="ag")
	private int AG;
	
	@Column(name="av")
	private int AV;
	
	@Column(name="cost")
	private int cost;
	
	@Column(name="imgurl")
	private String imgUrl;
	
	@ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
	@JoinTable(name="playertype_skill",
	           joinColumns=@JoinColumn(name="playertype_id"),
	           inverseJoinColumns=@JoinColumn(name="skill_id"))
	private List<Skill> skills;
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getMA() {
		return MA;
	}
	public void setMA(int mA) {
		MA = mA;
	}
	public int getST() {
		return ST;
	}
	public void setST(int sT) {
		ST = sT;
	}
	public int getAG() {
		return AG;
	}
	public void setAG(int aG) {
		AG = aG;
	}
	public int getAV() {
		return AV;
	}
	public void setAV(int aV) {
		AV = aV;
	}
	public int getCost() {
		return cost;
	}
	public void setCost(int cost) {
		this.cost = cost;
	}
	public List<Skill> getSkills() {
		return skills;
	}
	public void setSkills(List<Skill> skills) {
		this.skills = skills;
	}
	public String getImgUrl() {
		return imgUrl;
	}
	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	
}
