package com.yuva.gymtracker.service;

import com.yuva.gymtracker.dto.GoogleAuthRequest;
import com.yuva.gymtracker.dto.LoginResponseDto;
import com.yuva.gymtracker.entity.User;
import com.yuva.gymtracker.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleIdentityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final String googleClientId;
    private final String registrationCode;

    public GoogleIdentityService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 ObjectProvider<JwtDecoder> jwtDecoderProvider,
                                 @Value("${app.google.client-id:}") String googleClientId,
                                 @Value("${app.registration-code:}") String registrationCode) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.googleClientId = googleClientId;
        this.registrationCode = registrationCode;
    }

    public String getClientId() {
        return googleClientId;
    }

    @Transactional
    public LoginResponseDto authenticate(GoogleAuthRequest request) {
        JwtDecoder decoder = jwtDecoderProvider.getIfAvailable();
        if (googleClientId.isBlank() || decoder == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured yet");
        }

        Jwt identity;
        try {
            identity = decoder.decode(request.getCredential());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google sign-in could not be verified. Please try again.");
        }

        String googleSubject = identity.getSubject();
        String email = identity.getClaimAsString("email");
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (googleSubject == null || googleSubject.isBlank()
                || normalizedEmail.isBlank()
                || !Boolean.TRUE.equals(identity.getClaim("email_verified"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google did not provide a verified email for this account");
        }

        User user = userRepository.findByGoogleSubject(googleSubject).orElse(null);
        if (user == null) {
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This email already has an account. Sign in with its existing method.");
            }
            if (registrationCode.isBlank() || !registrationCode.equals(request.getRegistrationCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A valid beta access code is required to create a Google account");
            }

            String name = identity.getClaimAsString("name");
            if (name == null || name.isBlank()) {
                name = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
            }
            user = User.builder()
                    .name(name.trim())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()))
                    .googleSubject(googleSubject)
                    .build();
            user = userRepository.save(user);
        }

        return LoginResponseDto.builder()
                .token(jwtService.generateToken(user))
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .authMethod("GOOGLE")
                .build();
    }
}
