package com.trimly.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trimly.api.model.entity.QrCode;
import com.trimly.api.model.entity.Url;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QrCodeResponse {

    private Long id;

    @JsonProperty("_id")
    public String getMongoCompatibleId() {
        return id != null ? String.valueOf(id) : null;
    }

    private String userId;
    private String shortCode;
    private Map<String, Object> qrConfig;
    private Instant createdAt;

    private String title;
    private String destinationUrl;
    private List<String> tags;
    private Boolean visibleAsLink;
    private Boolean isHidden;
    private String shortUrl;
    private String longUrl;
    private Instant expiresAt;
    private String svg;
    private String svgUrl;

    public static QrCodeResponse fromEntity(QrCode qr, Url url, String baseUrl) {
        return fromEntity(qr, url, baseUrl, null);
    }

    public static QrCodeResponse fromEntity(QrCode qr, Url url, String baseUrl, String svg) {
        if (qr == null) return null;

        Map<String, Object> config = new HashMap<>();
        config.put("dotsStyle", qr.getDotsStyle());
        config.put("cornersStyle", qr.getCornersStyle());
        config.put("cornersDotStyle", qr.getCornersDotStyle());
        config.put("dotsColor", qr.getDotsColor());
        config.put("backgroundColor", qr.getBackgroundColor());
        config.put("logoUrl", qr.getLogoUrl());
        config.put("centerText", qr.getCenterText());

        String sUrl = (baseUrl != null ? baseUrl : "http://localhost:4000") + "/" + qr.getShortCode();
        String svgLink = (baseUrl != null ? baseUrl : "http://localhost:4000") + "/api/qrcodes/" + qr.getShortCode();

        return QrCodeResponse.builder()
                .id(qr.getId())
                .userId(String.valueOf(qr.getUserId()))
                .shortCode(qr.getShortCode())
                .qrConfig(config)
                .createdAt(qr.getCreatedAt())
                .title(url != null ? url.getTitle() : null)
                .destinationUrl(url != null ? url.getLongUrl() : null)
                .longUrl(url != null ? url.getLongUrl() : null)
                .tags(url != null ? url.getTags() : null)
                .visibleAsLink(url != null ? url.getVisibleAsLink() : false)
                .isHidden(qr.getIsHidden())
                .shortUrl(sUrl)
                .expiresAt(url != null ? url.getExpiresAt() : null)
                .svg(svg)
                .svgUrl(svgLink)
                .build();
    }
}
