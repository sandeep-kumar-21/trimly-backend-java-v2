package com.trimly.api.controller;

import com.trimly.api.dto.request.LoginRequest;
import com.trimly.api.dto.request.RegisterRequest;
import com.trimly.api.dto.response.AuthResponse;
import com.trimly.api.dto.response.UserResponse;
import com.trimly.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "User authentication and profile retrieval endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User successfully registered.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    @ApiResponse(responseCode = "409", description = "Email is already registered.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with user credentials")
    @ApiResponse(responseCode = "200", description = "User successfully logged in.")
    @ApiResponse(responseCode = "401", description = "Invalid email or password.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get profile of current authenticated user")
    @ApiResponse(responseCode = "200", description = "Returns authenticated user profile.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    public ResponseEntity<Map<String, UserResponse>> getCurrentUser() {
        UserResponse user = authService.getCurrentUser();
        return ResponseEntity.ok(Map.of("user", user));
    }
}
