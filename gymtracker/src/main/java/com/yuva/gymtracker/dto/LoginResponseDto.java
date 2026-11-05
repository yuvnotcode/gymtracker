package com.yuva.gymtracker.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LoginResponseDto {

    private String token;
    private Long id;
    private String name;
    private String email;
}
