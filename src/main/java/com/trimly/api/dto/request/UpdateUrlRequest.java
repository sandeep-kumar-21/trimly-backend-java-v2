package com.trimly.api.dto.request;

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
public class UpdateUrlRequest {

    @URL(message = "Invalid URL format. Must start with http:// or https://")
    private String longUrl;

    private Instant expiresAt;
    private String title;
    private Long campaignId;
    private String channel;
    private List<String> tags;
    private String password;
    private Boolean isHidden;
    private Boolean visibleAsLink;
}
