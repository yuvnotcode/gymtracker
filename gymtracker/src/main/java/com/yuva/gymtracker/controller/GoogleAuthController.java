package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.dto.GoogleAuthConfigResponse;
import com.yuva.gymtracker.dto.GoogleAuthRequest;
import com.yuva.gymtracker.dto.LoginResponseDto;
import com.yuva.gymtracker.service.GoogleIdentityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/google")
public class GoogleAuthController {
    private final GoogleIdentityService googleIdentityService;

    public GoogleAuthController(GoogleIdentityService googleIdentityService) {
        this.googleIdentityService = googleIdentityService;
    }

    @GetMapping("/config")
    public GoogleAuthConfigResponse getConfig() {
        return new GoogleAuthConfigResponse(googleIdentityService.getClientId());
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody GoogleAuthRequest request) {
        return googleIdentityService.authenticate(request);
    }
}
