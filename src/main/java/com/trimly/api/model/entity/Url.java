package com.trimly.api.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_urls_short_code", columnList = "short_code", unique = true),
        @Index(name = "idx_urls_user_visible_created", columnList = "user_id, visible_as_link, created_at"),
        @Index(name = "idx_urls_campaign", columnList = "campaign_id")
})
public class Url extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 50)
    private String shortCode;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(length = 50)
    private String channel;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(length = 255)
    private String title;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @Column(name = "password_hash")
    private String passwordHash;

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

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Builder.Default
    @Column(name = "visible_as_link", nullable = false)
    private Boolean visibleAsLink = true;

    @Builder.Default
    @Column(name = "has_qr", nullable = false)
    private Boolean hasQR = false;

    @Column(name = "qr_code_id")
    private Long qrCodeId;

    @Builder.Default
    @Column(name = "is_custom_alias", nullable = false)
    private Boolean isCustomAlias = false;
}
