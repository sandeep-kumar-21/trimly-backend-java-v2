package com.trimly.api.service.impl;

import com.trimly.api.dto.request.*;
import com.trimly.api.dto.response.UrlResponse;
import com.trimly.api.exception.BadRequestException;
import com.trimly.api.exception.ConflictException;
import com.trimly.api.exception.ForbiddenException;
import com.trimly.api.exception.ResourceNotFoundException;
import com.trimly.api.model.entity.QrCode;
import com.trimly.api.model.entity.Url;
import com.trimly.api.repository.QrCodeRepository;
import com.trimly.api.repository.UrlRepository;
import com.trimly.api.service.UrlService;
import com.trimly.api.util.Base62Util;
import com.trimly.api.util.TitleScraperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final QrCodeRepository qrCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.base-url:http://localhost:4000}")
    private String baseUrl;

    @Override
    @Transactional
    public UrlResponse createUrl(CreateUrlRequest request, Long userId) {
        String shortCode;
        boolean isCustom = false;

        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            shortCode = request.getCustomAlias().trim();
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new ConflictException("Custom alias is already taken");
            }
            isCustom = true;
        } else {
            Long nextSeq = urlRepository.getNextUrlSequenceValue();
            shortCode = Base62Util.encode(nextSeq);
        }

        // Auto-scrape page title if not explicitly provided
        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = TitleScraperUtil.scrapeTitle(request.getLongUrl());
        }

        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            passwordHash = passwordEncoder.encode(request.getPassword().trim());
        }

        Url url = Url.builder()
                .shortCode(shortCode)
                .longUrl(request.getLongUrl().trim())
                .userId(userId)
                .campaignId(request.getCampaignId())
                .channel(request.getChannel())
                .title(title)
                .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                .passwordHash(passwordHash)
                .utmSource(request.getUtmSource())
                .utmMedium(request.getUtmMedium())
                .utmCampaign(request.getUtmCampaign())
                .utmTerm(request.getUtmTerm())
                .utmContent(request.getUtmContent())
                .expiresAt(request.getExpiresAt())
                .isCustomAlias(isCustom)
                .hasQR(Boolean.TRUE.equals(request.getGenerateQrCode()))
                .build();

        Url savedUrl = urlRepository.save(url);

        // Auto-create QR Code if requested
        if (Boolean.TRUE.equals(request.getGenerateQrCode()) && userId != null) {
            QrCode qrCode = QrCode.builder()
                    .userId(userId)
                    .shortCode(shortCode)
                    .build();
            QrCode savedQr = qrCodeRepository.save(qrCode);
            savedUrl.setQrCodeId(savedQr.getId());
            savedUrl = urlRepository.save(savedUrl);
        }

        log.info("Created short URL {} -> {} (User: {})", shortCode, request.getLongUrl(), userId);
        return UrlResponse.fromEntity(savedUrl, baseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrlResponse> getUserUrls(Long userId, List<String> tags, String linkType, String qrAttachment) {
        List<Url> urls = urlRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return urls.stream()
                .filter(u -> {
                    // Filter by tags
                    if (tags != null && !tags.isEmpty()) {
                        if (u.getTags() == null || !new HashSet<>(u.getTags()).containsAll(tags)) {
                            return false;
                        }
                    }
                    // Filter by link type
                    if ("custom".equalsIgnoreCase(linkType) && !Boolean.TRUE.equals(u.getIsCustomAlias())) {
                        return false;
                    }
                    if ("auto".equalsIgnoreCase(linkType) && Boolean.TRUE.equals(u.getIsCustomAlias())) {
                        return false;
                    }
                    // Filter by QR attachment
                    if ("with".equalsIgnoreCase(qrAttachment) && !Boolean.TRUE.equals(u.getHasQR())) {
                        return false;
                    }
                    if ("without".equalsIgnoreCase(qrAttachment) && Boolean.TRUE.equals(u.getHasQR())) {
                        return false;
                    }
                    return true;
                })
                .map(u -> UrlResponse.fromEntity(u, baseUrl))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctTags(Long userId) {
        return urlRepository.findDistinctTagsByUserId(userId);
    }

    @Override
    @Transactional
    public Map<String, Object> bulkUpdateTags(Long userId, BulkTagsRequest request) {
        int updatedCount = 0;
        for (String idStr : request.getLinkIds()) {
            Long id = parseLongId(idStr);
            Optional<Url> urlOpt = urlRepository.findById(id);
            if (urlOpt.isPresent()) {
                Url url = urlOpt.get();
                verifyOwnership(url, userId);

                List<String> currentTags = url.getTags() != null ? new ArrayList<>(url.getTags()) : new ArrayList<>();
                switch (request.getAction().toLowerCase()) {
                    case "remove" -> currentTags.removeAll(request.getTags());
                    case "replace" -> {
                        currentTags.clear();
                        currentTags.addAll(request.getTags());
                    }
                    default -> { // "add"
                        for (String tag : request.getTags()) {
                            if (!currentTags.contains(tag)) currentTags.add(tag);
                        }
                    }
                }
                url.setTags(currentTags);
                urlRepository.save(url);
                updatedCount++;
            }
        }
        return Map.of("success", true, "updated", updatedCount);
    }

    @Override
    @Transactional
    public Map<String, Object> bulkHideUrls(Long userId, BulkHideRequest request) {
        int updatedCount = 0;
        for (String idStr : request.getLinkIds()) {
            Long id = parseLongId(idStr);
            Optional<Url> urlOpt = urlRepository.findById(id);
            if (urlOpt.isPresent()) {
                Url url = urlOpt.get();
                verifyOwnership(url, userId);
                url.setIsHidden(request.getHide());
                urlRepository.save(url);
                updatedCount++;
            }
        }
        return Map.of("success", true, "updated", updatedCount);
    }

    @Override
    @Transactional
    public UrlResponse promoteToLink(String code, Long userId) {
        Url url = getUrlOrThrow(code);
        verifyOwnership(url, userId);
        url.setVisibleAsLink(true);
        Url updated = urlRepository.save(url);
        return UrlResponse.fromEntity(updated, baseUrl);
    }

    @Override
    @Transactional
    public UrlResponse editBackHalf(String code, Long userId, EditBackHalfRequest request) {
        Url sourceUrl = getUrlOrThrow(code);
        verifyOwnership(sourceUrl, userId);

        String newAlias = request.getCustomBackHalf().trim();
        if (urlRepository.existsByShortCode(newAlias)) {
            throw new ConflictException("Custom alias is already taken");
        }

        Url cloned = Url.builder()
                .shortCode(newAlias)
                .longUrl(sourceUrl.getLongUrl())
                .userId(userId)
                .campaignId(sourceUrl.getCampaignId())
                .channel(sourceUrl.getChannel())
                .title(sourceUrl.getTitle())
                .tags(sourceUrl.getTags() != null ? new ArrayList<>(sourceUrl.getTags()) : new ArrayList<>())
                .passwordHash(sourceUrl.getPasswordHash())
                .utmSource(sourceUrl.getUtmSource())
                .utmMedium(sourceUrl.getUtmMedium())
                .utmCampaign(sourceUrl.getUtmCampaign())
                .utmTerm(sourceUrl.getUtmTerm())
                .utmContent(sourceUrl.getUtmContent())
                .expiresAt(sourceUrl.getExpiresAt())
                .isCustomAlias(true)
                .build();

        Url saved = urlRepository.save(cloned);
        return UrlResponse.fromEntity(saved, baseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public UrlResponse getUrlMetadata(String code) {
        Url url = getUrlOrThrow(code);
        return UrlResponse.fromEntity(url, baseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> verifyPassword(String code, String password) {
        Url url = getUrlOrThrow(code);

        if (url.getPasswordHash() == null || url.getPasswordHash().isEmpty()) {
            return Map.of("success", true, "longUrl", url.getLongUrl());
        }

        if (!passwordEncoder.matches(password, url.getPasswordHash())) {
            throw new BadRequestException("Invalid password");
        }

        return Map.of("success", true, "longUrl", url.getLongUrl());
    }

    @Override
    @Transactional
    public UrlResponse updateUrl(String code, UpdateUrlRequest request, Long userId) {
        Url url = getUrlOrThrow(code);
        verifyOwnership(url, userId);

        if (request.getLongUrl() != null) url.setLongUrl(request.getLongUrl().trim());
        if (request.getTitle() != null) url.setTitle(request.getTitle().trim());
        if (request.getTags() != null) url.setTags(new ArrayList<>(request.getTags()));
        if (request.getExpiresAt() != null) url.setExpiresAt(request.getExpiresAt());
        if (request.getCampaignId() != null) url.setCampaignId(request.getCampaignId());
        if (request.getChannel() != null) url.setChannel(request.getChannel());
        if (request.getIsHidden() != null) url.setIsHidden(request.getIsHidden());
        if (request.getVisibleAsLink() != null) url.setVisibleAsLink(request.getVisibleAsLink());
        if (request.getPassword() != null) {
            url.setPasswordHash(request.getPassword().isEmpty() ? null : passwordEncoder.encode(request.getPassword()));
        }

        Url saved = urlRepository.save(url);
        redisTemplate.delete("url:" + code);

        return UrlResponse.fromEntity(saved, baseUrl);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteUrl(String code, Long userId) {
        Url url = getUrlOrThrow(code);
        verifyOwnership(url, userId);

        if (url.getQrCodeId() != null) {
            qrCodeRepository.deleteById(url.getQrCodeId());
            redisTemplate.delete("qr:" + code + ":*");
        }

        urlRepository.delete(url);
        redisTemplate.delete("url:" + code);

        return Map.of("success", true, "message", "Short URL deleted successfully");
    }

    private Url getUrlOrThrow(String code) {
        return urlRepository.findByShortCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + code));
    }

    private void verifyOwnership(Url url, Long userId) {
        if (url.getUserId() == null || !url.getUserId().equals(userId)) {
            throw new ForbiddenException("Forbidden: You do not have permission to modify this URL");
        }
    }

    private Long parseLongId(String idStr) {
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid ID format: " + idStr);
        }
    }
}
