package com.project.footbrawl.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.footbrawl.entity.Skill;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {
}
