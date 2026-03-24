package com.example.eaibackend.repository;

import com.example.eaibackend.model.ErrorQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ErrorQuestionRepository extends JpaRepository<ErrorQuestion, Integer> {

    List<ErrorQuestion> findByStudentId(Integer studentId);
    
    ErrorQuestion findByStudentIdAndQuestionId(Integer studentId, Integer questionId);
}
