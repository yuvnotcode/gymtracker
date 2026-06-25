package com.yuva.gymtracker.dto;

import com.yuva.gymtracker.entity.Goal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private Integer age;
    private Double currentWeight;
    private Goal goal;
}
