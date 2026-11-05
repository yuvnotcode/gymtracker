package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.dto.LoginRequestDto;
import com.yuva.gymtracker.dto.LoginResponseDto;
import com.yuva.gymtracker.dto.UserRequestDto;
import com.yuva.gymtracker.dto.UserResponseDto;
import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @PostMapping("/register")
    public UserResponseDto registerUser(@Valid @RequestBody UserRequestDto requestDto){
        User user = User.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail().trim().toLowerCase(java.util.Locale.ROOT))
                .password(requestDto.getPassword())
                .age(requestDto.getAge())
                .currentWeight(requestDto.getCurrentWeight())
                .goal(requestDto.getGoal())
                .build();
        User savedUser = userService.saveUser(user);
        UserResponseDto reponse= UserResponseDto.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .age(savedUser.getAge())
                .currentWeight(savedUser.getCurrentWeight())
                .goal(savedUser.getGoal())
                .build();
        return reponse;
    }
    @PostMapping("/login")
    public LoginResponseDto login(
            @Valid @RequestBody LoginRequestDto requestDto){
        return userService.login(requestDto);
    }
}
