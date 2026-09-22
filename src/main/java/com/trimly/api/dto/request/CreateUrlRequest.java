package com.trimly.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "Destination URL is required")
    @URL(message = "Invalid URL format. Must start with http:// or https://")
    private String longUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", message = "Custom alias must be 3-50 characters containing only letters, numbers, hyphens, and underscores")
    private String customAlias;

    private String title;
    private List<String> tags;
    private String password;
    private Long campaignId;
    private String channel;
    private Instant expiresAt;

    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;

    @Builder.Default
    private Boolean generateQrCode = false;
}
