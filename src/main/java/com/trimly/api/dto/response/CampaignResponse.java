package com.trimly.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trimly.api.model.entity.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignResponse {

    private Long id;

    @JsonProperty("_id")
    public String getMongoCompatibleId() {
        return id != null ? String.valueOf(id) : null;
    }

    private String name;
    private String description;
    private List<String> channels;
    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
    private Long totalLinks;
    private Long totalClicks;
    private Map<String, Object> topChannel;

    public static CampaignResponse fromEntity(Campaign campaign, Long totalLinks, Long totalClicks, Map<String, Object> topChannel) {
        if (campaign == null) return null;
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .channels(campaign.getChannels())
                .userId(String.valueOf(campaign.getUserId()))
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .totalLinks(totalLinks != null ? totalLinks : 0L)
                .totalClicks(totalClicks != null ? totalClicks : 0L)
                .topChannel(topChannel)
                .build();
    }
}
