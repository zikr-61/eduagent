package com.example.eaibackend.repository;

import com.example.eaibackend.model.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentClassRepository extends JpaRepository<StudentClass, Integer> {

    List<StudentClass> findByStudentId(Integer studentId);
    
    List<StudentClass> findByClassId(Integer classId);
    
    StudentClass findByStudentIdAndClassId(Integer studentId, Integer classId);
}