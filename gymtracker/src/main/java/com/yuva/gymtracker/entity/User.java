package com.yuva.gymtracker.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.ArrayList;

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

    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    @Min(13)
    private Integer age;

    private Double currentWeight;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", length = 32, columnDefinition = "varchar(32)")
    private SplitType splitType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_custom_split_days", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "day_order")
    @Column(name = "day_name", nullable = false, length = 40)
    @Builder.Default
    private List<String> customSplitDays = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonManagedReference
    private List<Workout> workouts;
}
