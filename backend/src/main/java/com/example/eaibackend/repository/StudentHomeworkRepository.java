package com.example.eaibackend.repository;

import com.example.eaibackend.model.StudentHomework;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentHomeworkRepository extends JpaRepository<StudentHomework, Integer> {

    List<StudentHomework> findByStudentId(Integer studentId);
    
    List<StudentHomework> findByStudentIdAndStatus(Integer studentId, String status);
    
    List<StudentHomework> findByHomeworkId(Integer homeworkId);
    
    StudentHomework findByStudentIdAndHomeworkId(Integer studentId, Integer homeworkId);
}