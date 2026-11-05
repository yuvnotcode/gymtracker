package com.yuva.gymtracker.dto;

import com.yuva.gymtracker.entity.Goal;
import lombok.*;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {
    @NotBlank private String name;
    @NotBlank @Size(min = 8, message = "must be at least 8 characters")
    private String password;
    @NotBlank @Email
    private String email;
    @Min(13) @Max(120)
    private Integer age;
    @Positive
    private Double currentWeight;
    private Goal goal;
}
