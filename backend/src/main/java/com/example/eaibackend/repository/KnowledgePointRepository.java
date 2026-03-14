package com.example.eaibackend.repository;

import com.example.eaibackend.model.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Integer> {

    List<KnowledgePoint> findByUserId(Integer userId);

    List<KnowledgePoint> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
