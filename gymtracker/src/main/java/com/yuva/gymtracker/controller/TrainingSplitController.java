package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.dto.SplitPreferenceRequest;
import com.yuva.gymtracker.dto.SplitPreferenceResponse;
import com.yuva.gymtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users/me/split")
public class TrainingSplitController {
    private final UserService userService;

    public TrainingSplitController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public SplitPreferenceResponse getSplit(Authentication authentication) {
        return userService.getSplitPreference(authentication.getName());
    }

    @PutMapping
    public SplitPreferenceResponse updateSplit(@Valid @RequestBody SplitPreferenceRequest request,
                                               Authentication authentication) {
        return userService.updateSplitPreference(authentication.getName(), request);
    }
}
