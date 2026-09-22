package com.trimly.api.service.impl;

import com.trimly.api.exception.ResourceNotFoundException;
import com.trimly.api.model.entity.Url;
import com.trimly.api.repository.UrlRepository;
import com.trimly.api.service.AsyncClickService;
import com.trimly.api.service.RedirectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;
    private final AsyncClickService asyncClickService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final String URL_CACHE_PREFIX = "url:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String PASSWORD_PROTECTED_MARKER = "__PASSWORD_PROTECTED__";

    @Override
    public RedirectResult resolveRedirect(
            String code,
            String clientIp,
            String referrer,
            String userAgent,
            boolean isQrScan,
            Map<String, String> utms) {
        String cacheKey = URL_CACHE_PREFIX + code;
        String protectedRedirectUrl = frontendUrl + "/protected/" + code;

        // 1. Read-through Redis Cache
        Object cachedValue = null;
        try {
            cachedValue = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception ex) {
            log.warn("Redis lookup failed for key {}: {}", cacheKey, ex.getMessage());
        }

        if (PASSWORD_PROTECTED_MARKER.equals(cachedValue)) {
            return new RedirectResult(null, true, protectedRedirectUrl);
        }

        Url url = null;
        String longUrl = null;

        if (cachedValue instanceof String strUrl) {
            longUrl = strUrl;
        } else {
            // 2. Query PostgreSQL on cache miss
            url = urlRepository.findByShortCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));

            // Verify Expiration
            if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now())) {
                throw new ResourceNotFoundException("Short URL has expired");
            }

            // Verify Password Protection
            if (url.getPasswordHash() != null && !url.getPasswordHash().isEmpty()) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, PASSWORD_PROTECTED_MARKER, CACHE_TTL);
                } catch (Exception ex) {
                    log.warn("Redis cache set failed for key {}: {}", cacheKey, ex.getMessage());
                }
                return new RedirectResult(null, true, protectedRedirectUrl);
            }

            longUrl = url.getLongUrl();

            // Populate Redis cache
            try {
                redisTemplate.opsForValue().set(cacheKey, longUrl, CACHE_TTL);
            } catch (Exception ex) {
                log.warn("Redis cache set failed for key {}: {}", cacheKey, ex.getMessage());
            }
        }

        // 3. Dispatch Asynchronous Click Telemetry
        Long userId = url != null ? url.getUserId() : null;
        Long campaignId = url != null ? url.getCampaignId() : null;

        asyncClickService.recordClickAsync(
                code,
                userId,
                campaignId,
                clientIp,
                referrer,
                userAgent,
                isQrScan,
                utms
        );

        return new RedirectResult(longUrl, false, null);
    }
}
