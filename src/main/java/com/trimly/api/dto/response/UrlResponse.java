package com.trimly.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trimly.api.model.entity.Url;
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
public class UrlResponse {

    private Long id;

    @JsonProperty("_id")
    public String getMongoCompatibleId() {
        return id != null ? String.valueOf(id) : null;
    }

    private String shortCode;
    private String shortUrl;
    private String longUrl;
    private Long clickCount;
    private Instant createdAt;
    private Instant expiresAt;

    private String userId;
    private String title;
    private List<String> tags;

    private boolean passwordProtected;
    private String campaignId;
    private String channel;

    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;

    private Boolean isHidden;
    private Boolean customBackHalf;
    private Boolean visibleAsLink;

    @JsonProperty("hasQR")
    private Boolean hasQR;

    private String qrCodeId;
    private Boolean isCustomAlias;

    public static UrlResponse fromEntity(Url url, String baseUrl) {
        if (url == null) {
            return null;
        }

        String shortUrl = (baseUrl != null ? baseUrl : "http://localhost:4000") + "/" + url.getShortCode();

        return UrlResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .shortUrl(shortUrl)
                .longUrl(url.getLongUrl())
                .clickCount(url.getClickCount() != null ? url.getClickCount() : 0L)
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .userId(url.getUserId() != null ? String.valueOf(url.getUserId()) : null)
                .title(url.getTitle())
                .tags(url.getTags())
                .passwordProtected(url.getPasswordHash() != null && !url.getPasswordHash().isEmpty())
                .campaignId(url.getCampaignId() != null ? String.valueOf(url.getCampaignId()) : null)
                .channel(url.getChannel())
                .utmSource(url.getUtmSource())
                .utmMedium(url.getUtmMedium())
                .utmCampaign(url.getUtmCampaign())
                .utmTerm(url.getUtmTerm())
                .utmContent(url.getUtmContent())
                .isHidden(url.getIsHidden())
                .customBackHalf(url.getIsCustomAlias())
                .visibleAsLink(url.getVisibleAsLink())
                .hasQR(url.getHasQR())
                .qrCodeId(url.getQrCodeId() != null ? String.valueOf(url.getQrCodeId()) : null)
                .isCustomAlias(url.getIsCustomAlias())
                .build();
    }
}
