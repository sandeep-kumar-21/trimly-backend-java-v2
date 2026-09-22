package com.trimly.api.dto.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsDashboardResponse {

    private AnalyticsSummary summary;
    private List<TimeSeriesPoint> timeSeries;
    private LocationsData locations;
    private List<BreakdownMetric> referrers;
    private PlatformsData platforms;
    private UtmsData utms;
    private List<TopLinkMetric> topLinks;
    private List<LiveClickEvent> recentActivity;
    private List<Map<String, Object>> userUrls;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsSummary {
        private long totalClicks;
        private long prevTotalClicks;
        private double clicksGrowth;
        private long uniqueVisitors;
        private long prevUniqueVisitors;
        private double uniqueGrowth;
        private long qrScans;
        private long prevQrScans;
        private double qrGrowth;
        private long webClicks;
        private double qrPercentage;
        private String topCountry;
        private String topReferrer;
        private TopLinkMetric topLink;
        private String smartSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private String date;
        private long current;
        private long previous;
        private Long currentUniques;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownMetric {
        private String name;
        private long count;
        private double percentage;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationsData {
        private List<LocationCountryMetric> countries;
        private List<LocationCityMetric> cities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationCountryMetric {
        private String name;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationCityMetric {
        private String city;
        private String country;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformsData {
        private List<BreakdownMetric> devices;
        private List<BreakdownMetric> os;
        private List<BreakdownMetric> browsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtmsData {
        private List<UtmMetric> sources;
        private List<UtmMetric> mediums;
        private List<UtmMetric> campaigns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtmMetric {
        private String name;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopLinkMetric {
        private String shortCode;
        private String title;
        private String longUrl;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveClickEvent {
        private String shortCode;
        private String timestamp;
        private String country;
        private String city;
        private String region;
        private String deviceType;
        private String browser;
        private String os;
        private String referrer;
        private Boolean isQrScan;
        private String utmSource;
        private String utmMedium;
        private String utmCampaign;
        private String ipHash;
    }
}
