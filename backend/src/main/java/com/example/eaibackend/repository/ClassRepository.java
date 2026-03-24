package com.example.eaibackend.repository;

import com.example.eaibackend.model.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ClassRepository extends JpaRepository<Class, Integer> {

    Optional<Class> findByInviteCode(String inviteCode);
    
    List<Class> findByTeacherId(Integer teacherId);
}