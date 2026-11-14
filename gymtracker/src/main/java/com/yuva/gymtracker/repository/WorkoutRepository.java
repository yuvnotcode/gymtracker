package com.yuva.gymtracker.repository;

import com.yuva.gymtracker.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByUserId(Long userId);
    List<Workout> findByUserIdAndWorkoutDateAndWorkoutName(Long userId, LocalDate workoutDate, String workoutName);
    List<Workout> findByIdInAndUserId(List<Long> ids, Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from WorkoutSet s where s.workout.id in (select w.id from Workout w where w.user.id = :userId)")
    void deleteSetsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Workout w where w.user.id = :userId")
    void deleteWorkoutsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
