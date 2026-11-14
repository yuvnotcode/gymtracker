package com.yuva.gymtracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleAuthRequest {
    @NotBlank
    private String credential;
    private String registrationCode;
}
