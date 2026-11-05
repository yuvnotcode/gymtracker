package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.entity.Workout;
import com.yuva.gymtracker.dto.WorkoutSessionRequest;
import com.yuva.gymtracker.service.WorkoutService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;

@RestController
public class WorkoutController {
    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService){
        this.workoutService=workoutService;
    }
    @GetMapping("/users/{userId}/workouts")
    public List<Workout> getWorkoutsByUserId(@PathVariable Long userId, Authentication authentication){
        return workoutService.getWorkoutsByUserId(requireOwner(userId, authentication));
    }


    @PostMapping("/users/{userId}/workouts")
    public Workout createWorkout(@PathVariable Long userId,
                                 @RequestBody Workout workout, Authentication authentication){
        return workoutService.createWorkout(requireOwner(userId, authentication),workout);

    }

    @PostMapping("/users/{userId}/workouts/session")
    public List<Workout> createSession(@PathVariable Long userId,
                                       @RequestBody WorkoutSessionRequest request,
                                       Authentication authentication) {
        return workoutService.createSession(requireOwner(userId, authentication), request);
    }

    @DeleteMapping("/workouts/{id}")
    public void deleteWorkoutById(@PathVariable Long id, Authentication authentication){
        workoutService.deleteWorkoutById(id, authentication.getName());
    }

    @DeleteMapping("/users/{userId}/workouts/session")
    public void deleteSession(@PathVariable Long userId, @RequestParam List<Long> ids,
                              Authentication authentication) {
        workoutService.deleteSession(requireOwner(userId, authentication), ids);
    }

    private Long requireOwner(Long requestedUserId, Authentication authentication) {
        Long authenticatedUserId = workoutService.getUserId(authentication.getName());
        if (!authenticatedUserId.equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own workouts");
        }
        return authenticatedUserId;
    }
}
