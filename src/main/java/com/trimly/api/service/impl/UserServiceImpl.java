package com.trimly.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.dto.request.ChangePasswordRequest;
import com.trimly.api.dto.request.DeleteAccountRequest;
import com.trimly.api.dto.request.UpdatePreferencesRequest;
import com.trimly.api.dto.request.UpdateProfileRequest;
import com.trimly.api.dto.response.ExportJobResponse;
import com.trimly.api.dto.response.UserResponse;
import com.trimly.api.exception.BadRequestException;
import com.trimly.api.exception.ResourceNotFoundException;
import com.trimly.api.model.entity.User;
import com.trimly.api.repository.*;
import com.trimly.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final CampaignRepository campaignRepository;
    private final QrCodeRepository qrCodeRepository;
    private final ClickRepository clickRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String EXPORT_KEY_PREFIX = "export:job:";

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);

        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserResponse updatePreferences(Long userId, UpdatePreferencesRequest request) {
        User user = getUserOrThrow(userId);

        if (request.getTheme() != null) {
            user.setTheme(request.getTheme());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }
        if (request.getNotificationsEnabled() != null) {
            user.setNotificationsEnabled(request.getNotificationsEnabled());
        }

        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Incorrect current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);
    }

    @Override
    public ExportJobResponse initiateDataExport(Long userId) {
        User user = getUserOrThrow(userId);
        String jobId = UUID.randomUUID().toString();

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("profile", UserResponse.fromEntity(user));
        exportData.put("urls", urlRepository.findByUserIdOrderByCreatedAtDesc(userId));
        exportData.put("campaigns", campaignRepository.findByUserIdOrderByCreatedAtDesc(userId));
        exportData.put("qrCodes", qrCodeRepository.findByUserIdOrderByCreatedAtDesc(userId));

        ExportJobResponse response = ExportJobResponse.builder()
                .jobId(jobId)
                .status("completed")
                .message("Export completed successfully")
                .result(exportData)
                .build();

        redisTemplate.opsForValue().set(EXPORT_KEY_PREFIX + jobId, response, Duration.ofMinutes(15));
        log.info("Export data job {} generated for user ID: {}", jobId, userId);

        return response;
    }

    @Override
    public ExportJobResponse getExportStatus(String jobId) {
        Object raw = redisTemplate.opsForValue().get(EXPORT_KEY_PREFIX + jobId);
        if (raw == null) {
            throw new ResourceNotFoundException("Export job not found or expired");
        }

        return objectMapper.convertValue(raw, ExportJobResponse.class);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Password confirmation failed");
        }

        // 1. Delete associated clicks
        var userUrls = urlRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (var url : userUrls) {
            redisTemplate.delete("url:" + url.getShortCode());
        }

        // 2. Cascade delete QR codes, campaigns, and URLs
        var qrCodes = qrCodeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (var qr : qrCodes) {
            redisTemplate.delete("qr:" + qr.getShortCode() + ":*");
            qrCodeRepository.delete(qr);
        }

        var campaigns = campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);
        campaignRepository.deleteAll(campaigns);

        urlRepository.deleteAll(userUrls);

        // 3. Delete user entity
        userRepository.delete(user);
        log.info("Account permanently deleted for user ID: {}", userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
}
