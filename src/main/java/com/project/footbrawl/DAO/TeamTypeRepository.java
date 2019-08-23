package com.project.footbrawl.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.footbrawl.entity.TeamType;

@Repository
public interface TeamTypeRepository extends JpaRepository<TeamType, Integer> {
}
