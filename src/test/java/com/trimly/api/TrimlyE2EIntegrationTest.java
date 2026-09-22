package com.trimly.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.dto.request.CreateCampaignRequest;
import com.trimly.api.dto.request.CreateQrCodeRequest;
import com.trimly.api.dto.request.CreateUrlRequest;
import com.trimly.api.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Trimly Complete E2E Integration Suite")
class TrimlyE2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete E2E: Health -> Register -> Login -> Shorten URL -> Redirect -> Campaign -> QR Code")
    void shouldExecuteFullUserJourney() throws Exception {
        // 1. Health Check
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.services.database").value("connected"))
                .andExpect(jsonPath("$.services.redis").value("connected"));

        // 2. Register New User
        String uniqueEmail = "dev_" + System.currentTimeMillis() + "@trimly.io";
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(uniqueEmail)
                .password("Password123!")
                .name("Integration Tester")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user._id").isString())
                .andReturn();

        Map<?, ?> registerBody = objectMapper.readValue(registerResult.getResponse().getContentAsString(), Map.class);
        String token = (String) registerBody.get("accessToken");
        assertThat(token).isNotBlank();

        // 3. Shorten URL with JWT
        CreateUrlRequest urlReq = CreateUrlRequest.builder()
                .longUrl("https://spring.io/projects/spring-boot")
                .title("Spring Boot Framework")
                .tags(List.of("framework", "java"))
                .build();

        MvcResult urlResult = mockMvc.perform(post("/api/urls")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(urlReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$._id").isString())
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.hasQR").value(false))
                .andReturn();

        Map<?, ?> urlBody = objectMapper.readValue(urlResult.getResponse().getContentAsString(), Map.class);
        String shortCode = (String) urlBody.get("shortCode");
        assertThat(shortCode).isNotBlank();

        // 4. Test Redirection GET /{code}
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://spring.io/projects/spring-boot"));

        // 5. Create Marketing Campaign
        CreateCampaignRequest campaignReq = CreateCampaignRequest.builder()
                .name("Launch 2026")
                .description("Q3 Product Launch Campaign")
                .channels(List.of("email", "social"))
                .build();

        mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campaignReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$._id").isString())
                .andExpect(jsonPath("$.name").value("Launch 2026"));

        // 6. Generate QR Code
        CreateQrCodeRequest qrReq = CreateQrCodeRequest.builder()
                .shortCode(shortCode)
                .qrConfig(Map.of("dotsColor", "#0f172a", "backgroundColor", "#ffffff", "dotsStyle", "dots", "cornersStyle", "extra-rounded"))
                .build();

        MvcResult qrResult = mockMvc.perform(post("/api/qrcodes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qrReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCode.shortCode").value(shortCode))
                .andExpect(jsonPath("$.qrCode._id").isString())
                .andExpect(jsonPath("$.qrCode.svg").isString())
                .andExpect(jsonPath("$.svg").isString())
                .andReturn();

        Map<?, ?> qrBody = objectMapper.readValue(qrResult.getResponse().getContentAsString(), Map.class);
        String imageBase64 = (String) qrBody.get("imageBase64");
        String svg = (String) qrBody.get("svg");
        assertThat(imageBase64).startsWith("data:image/png;base64,");
        assertThat(svg).contains("<svg").contains(">trimly</text>").contains("viewBox=\"0 0 1000 1000\"");

        // 7. Verify GET /api/qrcodes returns SVG for frontend lists
        mockMvc.perform(get("/api/qrcodes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value(shortCode))
                .andExpect(jsonPath("$[0].svg").isString());

        // 8. Verify GET /api/qrcodes/{code}/details returns SVG
        mockMvc.perform(get("/api/qrcodes/" + shortCode + "/details")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.svg").isString());

        // 9. Verify GET /api/qrcodes/{code}?format=svg returns image/svg+xml
        mockMvc.perform(get("/api/qrcodes/" + shortCode + "?format=svg")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/svg+xml"));
    }
}
