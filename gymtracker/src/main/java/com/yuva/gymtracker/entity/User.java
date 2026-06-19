package com.yuva.gymtracker.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(unique = true,nullable = false)
    @Email
    @NotBlank
    private String email;

    @Column(nullable = false)
    private String password;

    @Min(13)
    private Integer age;

    private Double currentWeight;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference
    private List<Workout> workouts;
}
