package com.trimly.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDetailsResponse {

    private CampaignResponse campaign;
    private Long totalClicks;
    private Long totalLinks;
    private Map<String, Object> topChannel;
    private List<ChannelGroup> channels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelGroup {
        private String channel;
        private Long totalClicks;
        private Long totalLinks;
        private Double percentOfClicks;
        private List<UrlResponse> links;
    }
}
