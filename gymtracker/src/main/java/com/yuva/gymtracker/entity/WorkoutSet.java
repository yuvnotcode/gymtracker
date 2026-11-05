package com.yuva.gymtracker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_sets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutSet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "set_number", nullable = false)
    private Integer setNumber;
    @Column(nullable = false)
    private Integer reps;
    @Column(nullable = false)
    private Double weight;
    private Integer rir;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    @JsonIgnore
    private Workout workout;
}
