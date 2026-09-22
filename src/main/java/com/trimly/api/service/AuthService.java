package com.trimly.api.service;

import com.trimly.api.dto.request.LoginRequest;
import com.trimly.api.dto.request.RegisterRequest;
import com.trimly.api.dto.response.AuthResponse;
import com.trimly.api.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser();
}
