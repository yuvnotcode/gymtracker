package com.yuva.gymtracker.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WorkoutSessionRequest {
    private String workoutName;
    private LocalDate workoutDate;
    private String notes;
    private List<ExerciseEntry> exercises;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ExerciseEntry {
        private String exerciseName;
        private List<SetEntry> sets;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SetEntry {
        private Integer reps;
        private Double weight;
        private Integer rir;
    }
}
