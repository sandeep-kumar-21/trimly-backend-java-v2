package com.trimly.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.model.entity.Click;
import com.trimly.api.repository.ClickRepository;
import com.trimly.api.repository.UrlRepository;
import com.trimly.api.service.AsyncClickService;
import com.trimly.api.util.GeoIpResolverUtil;
import com.trimly.api.util.UserAgentParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncClickServiceImpl implements AsyncClickService {

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    @Override
    @Transactional
    public void recordClickAsync(
            String shortCode,
            Long userId,
            Long campaignId,
            String reqIp,
            String referrer,
            String userAgent,
            boolean isQrScan,
            Map<String, String> utms) {
        try {
            // 1. Atomically increment URL click count
            urlRepository.incrementClickCount(shortCode);

            // 2. Parse User-Agent & Geolocation
            UserAgentParserUtil.ParsedUserAgent ua = UserAgentParserUtil.parse(userAgent);
            GeoIpResolverUtil.GeoLocation geo = GeoIpResolverUtil.resolve(reqIp);
            String ipHash = hashIp(reqIp);

            // 3. Persist Click record in PostgreSQL
            Click click = Click.builder()
                    .shortCode(shortCode)
                    .userId(userId)
                    .campaignId(campaignId)
                    .timestamp(Instant.now())
                    .ipHash(ipHash)
                    .referrer(referrer)
                    .userAgent(userAgent)
                    .deviceType(ua.deviceType())
                    .browser(ua.browser())
                    .os(ua.os())
                    .country(geo.country())
                    .city(geo.city())
                    .region(geo.region())
                    .isQrScan(isQrScan)
                    .utmSource(utms != null ? utms.get("utmSource") : null)
                    .utmMedium(utms != null ? utms.get("utmMedium") : null)
                    .utmCampaign(utms != null ? utms.get("utmCampaign") : null)
                    .utmTerm(utms != null ? utms.get("utmTerm") : null)
                    .utmContent(utms != null ? utms.get("utmContent") : null)
                    .build();

            clickRepository.save(click);

            // 4. Publish live telemetry event via Redis Pub/Sub for SSE streaming
            if (userId != null) {
                Map<String, Object> livePayload = new HashMap<>();
                livePayload.put("shortCode", shortCode);
                livePayload.put("timestamp", click.getTimestamp().toString());
                livePayload.put("country", click.getCountry());
                livePayload.put("city", click.getCity());
                livePayload.put("region", click.getRegion());
                livePayload.put("deviceType", click.getDeviceType());
                livePayload.put("browser", click.getBrowser());
                livePayload.put("os", click.getOs());
                livePayload.put("referrer", click.getReferrer());
                livePayload.put("isQrScan", click.getIsQrScan());
                livePayload.put("utmSource", click.getUtmSource());
                livePayload.put("utmMedium", click.getUtmMedium());
                livePayload.put("utmCampaign", click.getUtmCampaign());
                livePayload.put("userId", userId);

                String channel = "analytics:live:" + userId;
                redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(livePayload));
            }

            log.debug("Processed click telemetry for link {} (User: {})", shortCode, userId);
        } catch (Exception ex) {
            log.error("Failed to process async click telemetry for {}: {}", shortCode, ex.getMessage(), ex);
        }
    }

    private String hashIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "anonymous";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
