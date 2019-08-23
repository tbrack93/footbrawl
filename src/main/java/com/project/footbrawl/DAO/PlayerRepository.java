package com.project.footbrawl.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.footbrawl.entity.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
}
