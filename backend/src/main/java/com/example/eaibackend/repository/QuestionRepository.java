package com.example.eaibackend.repository;

import com.example.eaibackend.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByKnowledgePointId(Integer knowledgePointId);

    List<Question> findByKnowledgePointIdOrderByCreatedAtDesc(Integer knowledgePointId);
}
