package com.trimly.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clicks", indexes = {
        @Index(name = "idx_clicks_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_clicks_code_timestamp", columnList = "short_code, timestamp"),
        @Index(name = "idx_clicks_campaign_timestamp", columnList = "campaign_id, timestamp"),
        @Index(name = "idx_clicks_user_qr_timestamp", columnList = "user_id, is_qr_scan, timestamp"),
        @Index(name = "idx_clicks_user_country", columnList = "user_id, country")
})
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 50)
    private String shortCode;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Builder.Default
    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Builder.Default
    @Column(name = "device_type", length = 30)
    private String deviceType = "Desktop";

    @Builder.Default
    @Column(length = 50)
    private String browser = "Unknown";

    @Builder.Default
    @Column(length = 50)
    private String os = "Unknown";

    @Builder.Default
    @Column(length = 10)
    private String country = "Unknown";

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Builder.Default
    @Column(name = "is_qr_scan", nullable = false)
    private Boolean isQrScan = false;

    @Column(name = "utm_source", length = 100)
    private String utmSource;

    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 100)
    private String utmCampaign;

    @Column(name = "utm_term", length = 100)
    private String utmTerm;

    @Column(name = "utm_content", length = 100)
    private String utmContent;
}
