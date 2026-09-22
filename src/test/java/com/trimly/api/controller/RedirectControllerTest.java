package com.trimly.api.controller;

import com.trimly.api.service.RedirectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Redirect Controller Integration Tests")
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedirectService redirectService;

    @Test
    @DisplayName("GET /{code} should return HTTP 302 Found with destination Location header")
    void shouldRedirectRootCode() throws Exception {
        when(redirectService.resolveRedirect(eq("w7k"), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new RedirectService.RedirectResult("https://target.com/page", false, null));

        mockMvc.perform(get("/w7k"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://target.com/page"));
    }

    @Test
    @DisplayName("GET /r/{code} should return HTTP 302 Found with destination Location header")
    void shouldRedirectRouteR() throws Exception {
        when(redirectService.resolveRedirect(eq("w7k"), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new RedirectService.RedirectResult("https://target.com/page", false, null));

        mockMvc.perform(get("/r/w7k"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://target.com/page"));
    }

    @Test
    @DisplayName("GET /{code} for password-protected link should redirect to frontend protected page")
    void shouldRedirectPasswordProtectedLink() throws Exception {
        when(redirectService.resolveRedirect(eq("secret123"), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new RedirectService.RedirectResult(null, true, "http://localhost:3000/protected/secret123"));

        mockMvc.perform(get("/secret123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000/protected/secret123"));
    }
}
