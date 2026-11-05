package com.yuva.gymtracker.repository;

import com.yuva.gymtracker.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByUserId(Long userId);
    List<Workout> findByUserIdAndWorkoutDateAndWorkoutName(Long userId, LocalDate workoutDate, String workoutName);
    List<Workout> findByIdInAndUserId(List<Long> ids, Long userId);
}
