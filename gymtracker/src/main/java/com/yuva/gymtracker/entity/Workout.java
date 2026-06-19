package com.yuva.gymtracker.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
@Entity
@Table(name = "workouts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Long id;
    private String workoutName;
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Double weight;
    private LocalDate workoutDate;
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

}
