package com.yuva.gymtracker.service;

import com.yuva.gymtracker.dto.LoginRequestDto;
import com.yuva.gymtracker.dto.LoginResponseDto;
import com.yuva.gymtracker.dto.SplitPreferenceRequest;
import com.yuva.gymtracker.dto.SplitPreferenceResponse;
import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.entity.SplitType;
import com.yuva.gymtracker.entity.Workout;
import com.yuva.gymtracker.repository.UserRepository;
import com.yuva.gymtracker.repository.WorkoutRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WorkoutRepository workoutRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       WorkoutRepository workoutRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService=jwtService;
        this.workoutRepository = workoutRepository;
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

    @Transactional
    public SplitPreferenceResponse getSplitPreference(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        if (user.getSplitType() == null) {
            user.setSplitType(SplitType.FULL_BODY);
            userRepository.save(user);
        }
        List<String> customDays = user.getCustomSplitDays() == null ? List.of() : user.getCustomSplitDays();
        return new SplitPreferenceResponse(user.getSplitType(), List.copyOf(customDays));
    }

    @Transactional
    public SplitPreferenceResponse updateSplitPreference(String email, SplitPreferenceRequest request) {
        if (request.getSplitType() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a training split");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        List<String> days = request.getSplitType() == SplitType.CUSTOM
                ? normalizeSplitDays(request.getCustomSplitDays())
                : new ArrayList<>(user.getCustomSplitDays() == null ? List.of() : user.getCustomSplitDays());
        user.setSplitType(request.getSplitType());
        if (user.getCustomSplitDays() == null) {
            user.setCustomSplitDays(new ArrayList<>(days));
        } else {
            user.getCustomSplitDays().clear();
            user.getCustomSplitDays().addAll(days);
        }
        userRepository.save(user);
        return new SplitPreferenceResponse(user.getSplitType(), List.copyOf(user.getCustomSplitDays()));
    }

    private List<String> normalizeSplitDays(List<String> requestedDays) {
        if (requestedDays == null || requestedDays.isEmpty() || requestedDays.size() > 7) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "A custom split must have between 1 and 7 days");
        }
        List<String> days = requestedDays.stream()
                .map(day -> day == null ? "" : day.trim())
                .toList();
        if (days.stream().anyMatch(day -> day.isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Split day names cannot be blank");
        }
        if (days.stream().allMatch(day -> day.equalsIgnoreCase("Rest"))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "A custom split needs at least one training day");
        }
        return days.stream().map(day -> day.equalsIgnoreCase("rest") ? "Rest" : day).toList();
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
               .authMethod("PASSWORD")
               .build();

    }

    @Transactional(readOnly = true)
    public String exportAccountCsv(String email) {
        User user = requireUser(email);
        StringBuilder csv = new StringBuilder("Name,Email,Age,Current Weight (kg),Goal,Split Type,Custom Split Days,Workout Date,Workout Name,Exercise,Set Number,Reps,Weight (kg),RIR\r\n");
        List<Workout> workouts = workoutRepository.findByUserId(user.getId()).stream()
                .sorted(java.util.Comparator.comparing(Workout::getWorkoutDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        String splitDays = user.getCustomSplitDays() == null ? "" : String.join(" | ", user.getCustomSplitDays());
        if (workouts.isEmpty()) {
            appendCsvRow(csv, user.getName(), user.getEmail(), user.getAge(), user.getCurrentWeight(),
                    user.getGoal(), user.getSplitType(), splitDays, "", "", "", "", "", "");
        }
        for (Workout workout : workouts) {
            if (workout.getSetDetails().isEmpty()) {
                appendCsvRow(csv, user.getName(), user.getEmail(), user.getAge(), user.getCurrentWeight(),
                        user.getGoal(), user.getSplitType(), splitDays, workout.getWorkoutDate(),
                        workout.getWorkoutName(), workout.getExerciseName(), "", "", "", "");
            } else {
                workout.getSetDetails().forEach(set -> appendCsvRow(csv, user.getName(), user.getEmail(),
                        user.getAge(), user.getCurrentWeight(), user.getGoal(), user.getSplitType(), splitDays,
                        workout.getWorkoutDate(), workout.getWorkoutName(), workout.getExerciseName(),
                        set.getSetNumber(), set.getReps(), set.getWeight(), set.getRir()));
            }
        }
        return "\uFEFF" + csv;
    }

    private void appendCsvRow(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(',');
            String value = values[i] == null ? "" : values[i].toString();
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    @Transactional
    public void deleteAccount(String email, String password, String confirmation) {
        User user = requireUser(email);
        if (!"DELETE".equals(confirmation)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Type DELETE to confirm account removal");
        }
        if (user.getGoogleSubject() == null
                && (password == null || !passwordEncoder.matches(password, user.getPassword()))) {
            throw new org.springframework.security.authentication.BadCredentialsException("Password is incorrect");
        }
        workoutRepository.deleteSetsByUserId(user.getId());
        workoutRepository.deleteWorkoutsByUserId(user.getId());
        userRepository.deleteCustomSplitDaysByUserId(user.getId());
        userRepository.delete(user);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
    }
}
