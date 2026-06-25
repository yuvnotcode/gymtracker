package com.yuva.gymtracker.service;

import com.yuva.gymtracker.dto.LoginRequestDto;
import com.yuva.gymtracker.dto.LoginResponseDto;
import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.exception.UserNotFoundException;
import com.yuva.gymtracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService=jwtService;
    }
    public User saveUser(User user){
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );
        return userRepository.save(user);
    }
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }
    public LoginResponseDto login(LoginRequestDto requestDto){
       User user = userRepository.findByEmail(requestDto.getEmail())
               .orElseThrow(()->new UserNotFoundException("User Not Found"));

       boolean isPasswordValid = passwordEncoder.matches(
               requestDto.getPassword(),
               user.getPassword()
       );

       if(!isPasswordValid){
           throw new RuntimeException("Invalid Password");
       }
//       return LoginResponseDto.builder()
//               .message("Login Successful")
//               .build();
        String token = jwtService.generateToken(user);
       return LoginResponseDto.builder()
               .token(jwtService.generateToken(user))
               .build();

    }
}
