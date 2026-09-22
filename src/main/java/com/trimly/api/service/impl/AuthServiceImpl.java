package com.trimly.api.service.impl;

import com.trimly.api.dto.request.LoginRequest;
import com.trimly.api.dto.request.RegisterRequest;
import com.trimly.api.dto.response.AuthResponse;
import com.trimly.api.dto.response.UserResponse;
import com.trimly.api.exception.ConflictException;
import com.trimly.api.exception.UnauthorizedException;
import com.trimly.api.model.entity.User;
import com.trimly.api.repository.UserRepository;
import com.trimly.api.security.JwtTokenProvider;
import com.trimly.api.security.SecurityUtils;
import com.trimly.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName() != null ? request.getName().trim() : null)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());

        log.info("Registered new user ID: {} with email: {}", savedUser.getId(), savedUser.getEmail());
        return AuthResponse.builder()
                .user(UserResponse.fromEntity(savedUser))
                .accessToken(token)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        log.info("User logged in successfully ID: {}", user.getId());
        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(token)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized: No authenticated user"));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return UserResponse.fromEntity(user);
    }
}
