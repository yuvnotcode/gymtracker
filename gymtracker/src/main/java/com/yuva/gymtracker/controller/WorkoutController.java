package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.entity.Workout;
import com.yuva.gymtracker.service.WorkoutService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkoutController {
    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService){
        this.workoutService=workoutService;
    }
    @GetMapping("/users/{userId}/workouts")
    public List<Workout> getWorkoutsByUserId(@PathVariable Long userId){
        return workoutService.getWorkoutsByUserId(userId);
    }


    @PostMapping("/users/{userId}/workouts")
    public Workout createWorkout(@PathVariable Long userId,
                                 @RequestBody Workout workout){
        return workoutService.createWorkout(userId,workout);

    }

    @DeleteMapping("/workouts/{id}")
    public void deleteWorkoutById(@PathVariable Long id){
        workoutService.deleteWorkoutById(id);
    }
}
