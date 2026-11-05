package com.yuva.gymtracker.service;

import com.yuva.gymtracker.dto.LoginRequestDto;
import com.yuva.gymtracker.dto.LoginResponseDto;
import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.entity.SplitType;
import com.yuva.gymtracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "An account with this email already exists");
        }
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );
        return userRepository.save(user);
    }
    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public SplitType getSplitType(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        if (user.getSplitType() == null) {
            user.setSplitType(SplitType.FULL_BODY);
            userRepository.save(user);
        }
        return user.getSplitType();
    }

    public SplitType updateSplitType(String email, SplitType splitType) {
        if (splitType == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a training split");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        user.setSplitType(splitType);
        userRepository.save(user);
        return splitType;
    }
    public LoginResponseDto login(LoginRequestDto requestDto){
       String email = requestDto.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
       User user = userRepository.findByEmail(email)
               .orElseThrow(()->new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));

       boolean isPasswordValid = passwordEncoder.matches(
               requestDto.getPassword(),
               user.getPassword()
       );

       if(!isPasswordValid){
           throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password");
       }
//       return LoginResponseDto.builder()
//               .message("Login Successful")
//               .build();
       return LoginResponseDto.builder()
               .token(jwtService.generateToken(user))
               .id(user.getId())
               .name(user.getName())
               .email(user.getEmail())
               .build();

    }
}
