package com.trimly.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.dto.request.UpdateProfileRequest;
import com.trimly.api.dto.response.UserResponse;
import com.trimly.api.security.UserPrincipal;
import com.trimly.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Controller Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("PATCH /api/users/profile should update and return UserResponse with dual IDs")
    void shouldUpdateProfile() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "user@trimly.io", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Updated Name")
                .avatarUrl("https://avatar.io/pic.png")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("user@trimly.io")
                .name("Updated Name")
                .avatarUrl("https://avatar.io/pic.png")
                .build();

        when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$._id").value("1"))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }
}
