package com.trimly.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.dto.request.LoginRequest;
import com.trimly.api.dto.request.RegisterRequest;
import com.trimly.api.dto.response.AuthResponse;
import com.trimly.api.dto.response.UserResponse;
import com.trimly.api.security.CustomUserDetailsService;
import com.trimly.api.security.JwtAuthenticationFilter;
import com.trimly.api.security.JwtTokenProvider;
import com.trimly.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Auth Controller Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register should return 201 with auth response")
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@trimly.io")
                .password("securePassword123")
                .name("Trimly Tester")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@trimly.io")
                .name("Trimly Tester")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .user(userResponse)
                .accessToken("mock-jwt-token")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user._id").value("1"))
                .andExpect(jsonPath("$.user.email").value("test@trimly.io"));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid email should return 400 Bad Request")
    void shouldRejectInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 with JWT token")
    void shouldLoginUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@trimly.io")
                .password("securePassword123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@trimly.io")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .user(userResponse)
                .accessToken("valid-jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("valid-jwt-token"))
                .andExpect(jsonPath("$.user._id").value("1"));
    }

    @Test
    @DisplayName("GET /api/auth/me without authentication should return 401/403")
    void shouldRejectUnauthenticatedMe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }
}
