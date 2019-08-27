package com.project.footbrawl.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.footbrawl.entity.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Integer> {
	// all CRUD actions provided by this, behind the scenes
}