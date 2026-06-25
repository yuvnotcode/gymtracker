package com.yuva.gymtracker.service;

import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.entity.Workout;
import com.yuva.gymtracker.exception.UserNotFoundException;
import com.yuva.gymtracker.repository.UserRepository;
import com.yuva.gymtracker.repository.WorkoutRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    public WorkoutService(WorkoutRepository workoutRepository, UserRepository userRepository){
        this.workoutRepository=workoutRepository;
        this.userRepository=userRepository;
    }

    public Workout createWorkout(Long userId, Workout workout){
       User user = userRepository.findById(userId)
               .orElseThrow(()->new UserNotFoundException("User not found"));
       workout.setUser(user);
       return workoutRepository.save(workout);
    }

    public List<Workout> getWorkoutsByUserId(Long userId){
        return workoutRepository.findByUserId(userId);
    }

    public void deleteWorkoutById(Long workoutId){
         workoutRepository.deleteById(workoutId);
    }

}
