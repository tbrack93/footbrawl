package com.project.footbrawl.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.footbrawl.entity.PlayerArchetype;

@Repository
public interface PlayerArchetypeRepository extends JpaRepository<PlayerArchetype, Integer> {
}
