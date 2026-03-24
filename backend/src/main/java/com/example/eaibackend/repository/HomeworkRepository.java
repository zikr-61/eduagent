package com.example.eaibackend.repository;

import com.example.eaibackend.model.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Integer> {

    List<Homework> findByTeacherId(Integer teacherId);

    List<Homework> findByTeacherIdOrderByCreatedAtDesc(Integer teacherId);
}
