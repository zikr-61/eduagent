package com.example.eaibackend.repository;

import com.example.eaibackend.model.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Integer> {

    List<StudyRecord> findByUserId(Integer userId);

    List<StudyRecord> findByUserIdAndRecordDateBetween(Integer userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(s.durationMinutes) FROM StudyRecord s WHERE s.userId = :userId")
    Integer getTotalDurationByUserId(@Param("userId") Integer userId);

    @Query("SELECT SUM(s.durationMinutes) FROM StudyRecord s WHERE s.userId = :userId AND s.recordDate BETWEEN :startDate AND :endDate")
    Integer getDurationByUserIdAndDateRange(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
