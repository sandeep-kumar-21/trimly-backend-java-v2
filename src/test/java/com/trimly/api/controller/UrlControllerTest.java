package com.trimly.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.dto.request.CreateUrlRequest;
import com.trimly.api.dto.response.UrlResponse;
import com.trimly.api.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("URL Controller Integration Tests")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlService urlService;

    @Test
    @DisplayName("POST /api/urls should shorten URL and return UrlResponse with dual IDs")
    void shouldCreateShortUrl() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("https://github.com/spring-projects/spring-boot")
                .title("Spring Boot")
                .build();

        UrlResponse response = UrlResponse.builder()
                .id(100L)
                .shortCode("w7k")
                .shortUrl("http://localhost:4000/w7k")
                .longUrl("https://github.com/spring-projects/spring-boot")
                .title("Spring Boot")
                .clickCount(0L)
                .visibleAsLink(true)
                .hasQR(false)
                .build();

        when(urlService.createUrl(any(CreateUrlRequest.class), isNull())).thenReturn(response);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$._id").value("100"))
                .andExpect(jsonPath("$.shortCode").value("w7k"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:4000/w7k"))
                .andExpect(jsonPath("$.hasQR").value(false));
    }

    @Test
    @DisplayName("POST /api/urls with invalid URL should return 400 Bad Request")
    void shouldRejectInvalidUrl() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("invalid-not-a-url")
                .build();

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/urls/{code} should return metadata for public view")
    void shouldGetUrlMetadata() throws Exception {
        UrlResponse response = UrlResponse.builder()
                .id(100L)
                .shortCode("w7k")
                .longUrl("https://example.com")
                .clickCount(42L)
                .build();

        when(urlService.getUrlMetadata("w7k")).thenReturn(response);

        mockMvc.perform(get("/api/urls/w7k"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("w7k"))
                .andExpect(jsonPath("$.clickCount").value(42));
    }
}
