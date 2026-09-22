package com.trimly.api.dto.response.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trimly.api.model.entity.Click;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickLogResponse {

    private Long id;

    @JsonProperty("_id")
    public String getMongoCompatibleId() {
        return id != null ? String.valueOf(id) : null;
    }

    private String shortCode;
    private Instant timestamp;
    private String ipHash;
    private String referrer;
    private String userAgent;
    private String deviceType;
    private String browser;
    private String os;
    private String country;
    private String city;
    private String region;
    private Boolean isQrScan;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;

    public static ClickLogResponse fromEntity(Click click) {
        if (click == null) return null;
        return ClickLogResponse.builder()
                .id(click.getId())
                .shortCode(click.getShortCode())
                .timestamp(click.getTimestamp())
                .ipHash(click.getIpHash())
                .referrer(click.getReferrer())
                .userAgent(click.getUserAgent())
                .deviceType(click.getDeviceType())
                .browser(click.getBrowser())
                .os(click.getOs())
                .country(click.getCountry())
                .city(click.getCity())
                .region(click.getRegion())
                .isQrScan(click.getIsQrScan())
                .utmSource(click.getUtmSource())
                .utmMedium(click.getUtmMedium())
                .utmCampaign(click.getUtmCampaign())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResult {
        private List<ClickLogResponse> data;
        private long total;
        private int page;
        private int limit;
        private int totalPages;
    }
}
