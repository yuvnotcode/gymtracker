package com.yuva.gymtracker.controller;

import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @GetMapping
    public List<User> getAllUsers(){
        return userService.getAllUsers();
    }

    @PostMapping("/register")
    public User registerUser(@Valid @RequestBody User user){
        return userService.saveUser(user);
    }
}
