package com.yuva.gymtracker.service;

import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }
    public User saveUser(User user){
        return userRepository.save(user);
    }
}
