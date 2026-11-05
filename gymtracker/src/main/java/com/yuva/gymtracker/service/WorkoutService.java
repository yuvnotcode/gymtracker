package com.yuva.gymtracker.service;

import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.entity.Workout;
import com.yuva.gymtracker.exception.UserNotFoundException;
import com.yuva.gymtracker.repository.UserRepository;
import com.yuva.gymtracker.repository.WorkoutRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import org.springframework.transaction.annotation.Transactional;
import com.yuva.gymtracker.dto.WorkoutSessionRequest;
import com.yuva.gymtracker.entity.WorkoutSet;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
       if (workout.getWorkoutDate() == null) workout.setWorkoutDate(java.time.LocalDate.now());
       if (workout.getWorkoutName() == null || workout.getWorkoutName().isBlank()
               || workout.getExerciseName() == null || workout.getExerciseName().isBlank()
               || workout.getSets() == null || workout.getSets() < 1
               || workout.getReps() == null || workout.getReps() < 1
               || workout.getWeight() == null || workout.getWeight() < 0) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workout, exercise, sets, reps and a non-negative weight are required");
       }
       workout.setId(null);
       workout.setUser(user);
       workout.setSetDetails(new ArrayList<>());
       return workoutRepository.save(workout);
    }

    @Transactional
    public List<Workout> createSession(Long userId, WorkoutSessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (request.getWorkoutName() == null || request.getWorkoutName().isBlank()
                || request.getWorkoutName().length() > 80
                || request.getExercises() == null || request.getExercises().isEmpty()
                || request.getExercises().size() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a workout name and between 1 and 30 exercises");
        }
        java.time.LocalDate date = request.getWorkoutDate() == null ? java.time.LocalDate.now() : request.getWorkoutDate();
        List<Workout> saved = new ArrayList<>();
        for (WorkoutSessionRequest.ExerciseEntry exercise : request.getExercises()) {
            if (exercise.getExerciseName() == null || exercise.getExerciseName().isBlank()
                    || exercise.getExerciseName().length() > 100 || exercise.getSets() == null
                    || exercise.getSets().isEmpty() || exercise.getSets().size() > 30) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each exercise needs a name and 1 to 30 sets");
            }
            Workout workout = Workout.builder().workoutName(request.getWorkoutName().trim())
                    .exerciseName(exercise.getExerciseName().trim()).workoutDate(date).user(user)
                    .sets(exercise.getSets().size()).build();
            int number = 1;
            for (WorkoutSessionRequest.SetEntry set : exercise.getSets()) {
                if (set.getReps() == null || set.getReps() < 1 || set.getReps() > 500
                        || set.getWeight() == null || set.getWeight() < 0 || set.getWeight() > 2000
                        || (set.getRir() != null && (set.getRir() < 0 || set.getRir() > 10))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Set reps, weight or reps-in-reserve are outside valid ranges");
                }
                workout.getSetDetails().add(WorkoutSet.builder().setNumber(number++)
                        .reps(set.getReps()).weight(set.getWeight()).rir(set.getRir()).workout(workout).build());
            }
            workout.setReps(workout.getSetDetails().getFirst().getReps());
            workout.setWeight(workout.getSetDetails().stream().mapToDouble(WorkoutSet::getWeight).max().orElse(0));
            saved.add(workoutRepository.save(workout));
        }
        return saved;
    }

    public List<Workout> getWorkoutsByUserId(Long userId){
        return workoutRepository.findByUserId(userId);
    }

    public void deleteWorkoutById(Long workoutId, String email) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));
        if (!workout.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own workouts");
        }
        workoutRepository.delete(workout);
    }

    @Transactional
    public void deleteSession(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No workout entries were selected");
        }
        List<Long> uniqueIds = ids.stream().distinct().toList();
        List<Workout> entries = workoutRepository.findByIdInAndUserId(uniqueIds, userId);
        if (entries.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout session not found");
        if (entries.size() != uniqueIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more workout entries could not be found");
        }
        workoutRepository.deleteAll(entries);
    }

    public Long getUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found")).getId();
    }

}
