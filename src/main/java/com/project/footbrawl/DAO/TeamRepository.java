package com.project.footbrawl.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.footbrawl.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
}
