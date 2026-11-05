package com.yuva.gymtracker.dto;

import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LoginRequestDto {

    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}
