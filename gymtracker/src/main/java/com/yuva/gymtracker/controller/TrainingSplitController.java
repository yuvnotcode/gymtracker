package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.dto.SplitPreferenceRequest;
import com.yuva.gymtracker.entity.SplitType;
import com.yuva.gymtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users/me/split")
public class TrainingSplitController {
    private final UserService userService;

    public TrainingSplitController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Map<String, SplitType> getSplit(Authentication authentication) {
        return Map.of("splitType", userService.getSplitType(authentication.getName()));
    }

    @PutMapping
    public Map<String, SplitType> updateSplit(@Valid @RequestBody SplitPreferenceRequest request,
                                             Authentication authentication) {
        return Map.of("splitType", userService.updateSplitType(authentication.getName(), request.getSplitType()));
    }
}
