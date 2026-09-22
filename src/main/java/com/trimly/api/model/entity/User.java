package com.trimly.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 150)
    private String name;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Builder.Default
    @Column(length = 20)
    private String theme = "system";

    @Builder.Default
    @Column(length = 50)
    private String timezone = "UTC";

    @Builder.Default
    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true;
}
