package com.trimly.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "qrcodes", indexes = {
        @Index(name = "idx_qrcodes_user", columnList = "user_id"),
        @Index(name = "idx_qrcodes_short_code", columnList = "short_code", unique = true)
})
public class QrCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "short_code", nullable = false, unique = true, length = 50)
    private String shortCode;

    @Builder.Default
    @Column(name = "dots_style", length = 30)
    private String dotsStyle = "square";

    @Builder.Default
    @Column(name = "corners_style", length = 30)
    private String cornersStyle = "square";

    @Builder.Default
    @Column(name = "corners_dot_style", length = 30)
    private String cornersDotStyle = "square";

    @Builder.Default
    @Column(name = "dots_color", length = 20)
    private String dotsColor = "#000000";

    @Builder.Default
    @Column(name = "background_color", length = 20)
    private String backgroundColor = "#FFFFFF";

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "center_text", length = 100)
    private String centerText;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;
}
