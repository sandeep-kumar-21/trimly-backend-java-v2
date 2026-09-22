package com.trimly.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trimly.api.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    @JsonProperty("_id")
    public String getMongoCompatibleId() {
        return id != null ? String.valueOf(id) : null;
    }

    private String email;
    private String name;
    private String avatarUrl;
    private String theme;
    private String timezone;
    private Boolean notificationsEnabled;
    private Instant createdAt;

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .theme(user.getTheme())
                .timezone(user.getTimezone())
                .notificationsEnabled(user.getNotificationsEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
