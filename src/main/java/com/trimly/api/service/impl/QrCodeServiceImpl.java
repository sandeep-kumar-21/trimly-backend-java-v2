package com.trimly.api.service.impl;

import com.trimly.api.dto.request.CreateQrCodeRequest;
import com.trimly.api.dto.request.UpdateQrCodeRequest;
import com.trimly.api.dto.response.QrCodeResponse;
import com.trimly.api.exception.BadRequestException;
import com.trimly.api.exception.ForbiddenException;
import com.trimly.api.exception.ResourceNotFoundException;
import com.trimly.api.model.entity.QrCode;
import com.trimly.api.model.entity.Url;
import com.trimly.api.repository.QrCodeRepository;
import com.trimly.api.repository.UrlRepository;
import com.trimly.api.service.QrCodeService;
import com.trimly.api.util.Base62Util;
import com.trimly.api.util.QrCodeGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.base-url:http://localhost:4000}")
    private String baseUrl;

    private static final String QR_CACHE_PREFIX = "qr:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Override
    @Transactional
    public Map<String, Object> createQrCode(CreateQrCodeRequest request, Long userId) {
        String shortCode = request.getShortCode();
        Url url;

        if (shortCode != null && !shortCode.trim().isEmpty()) {
            url = urlRepository.findByShortCode(shortCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
            verifyUrlOwnership(url, userId);
        } else if (request.getLongUrl() != null && !request.getLongUrl().trim().isEmpty()) {
            Long nextSeq = urlRepository.getNextUrlSequenceValue();
            shortCode = Base62Util.encode(nextSeq);
            url = Url.builder()
                    .shortCode(shortCode)
                    .longUrl(request.getLongUrl().trim())
                    .userId(userId)
                    .title(request.getTitle())
                    .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                    .visibleAsLink(Boolean.TRUE.equals(request.getCreateLink()))
                    .hasQR(true)
                    .build();
            url = urlRepository.save(url);
        } else {
            throw new BadRequestException("Either shortCode or longUrl must be provided to create a QR code");
        }

        Map<String, Object> config = request.getQrConfig() != null ? request.getQrConfig() : Collections.emptyMap();
        String dotsColor = config.get("dotsColor") instanceof String s && !s.isBlank() ? s.trim() : "#000000";
        String bgColor = config.get("backgroundColor") instanceof String s && !s.isBlank() ? s.trim() : "#FFFFFF";
        String dotsStyle = config.get("dotsStyle") instanceof String s && !s.isBlank() ? s.trim() : "square";
        String cornersStyle = config.get("cornersStyle") instanceof String s && !s.isBlank() ? s.trim() : "square";
        String cornersDotStyle = config.get("cornersDotStyle") instanceof String s && !s.isBlank() ? s.trim() : "square";
        String logoUrl = config.get("logoUrl") instanceof String s && !s.isBlank() ? s.trim() : null;
        String centerText = config.get("centerText") instanceof String s && !s.isBlank() ? s.trim() : null;

        QrCode qrCode = qrCodeRepository.findByShortCode(shortCode)
                .orElse(QrCode.builder().userId(userId).shortCode(shortCode).build());

        qrCode.setDotsColor(dotsColor);
        qrCode.setBackgroundColor(bgColor);
        qrCode.setDotsStyle(dotsStyle);
        qrCode.setCornersStyle(cornersStyle);
        qrCode.setCornersDotStyle(cornersDotStyle);
        qrCode.setLogoUrl(logoUrl);
        qrCode.setCenterText(centerText);

        QrCode savedQr = qrCodeRepository.save(qrCode);
        url.setHasQR(true);
        url.setQrCodeId(savedQr.getId());
        urlRepository.save(url);

        // Evict any stale Redis cache for this QR code so new styles are generated and cached
        evictQrCache(shortCode);

        String redirectUrl = baseUrl + "/r/" + shortCode + "?qr=1";
        byte[] pngBytes = QrCodeGeneratorUtil.generatePng(
                redirectUrl,
                dotsColor,
                bgColor,
                dotsStyle,
                cornersStyle,
                cornersDotStyle,
                centerText,
                logoUrl
        );
        String base64Image = QrCodeGeneratorUtil.toBase64DataUrl(pngBytes);

        String svg = generateSvgForQr(savedQr);
        QrCodeResponse response = QrCodeResponse.fromEntity(savedQr, url, baseUrl, svg);

        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", response);
        result.put("imageBase64", base64Image);
        result.put("svg", svg);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QrCodeResponse> getUserQrCodes(Long userId, String qrExpiration, String linkAttachment) {
        List<QrCode> qrCodes = qrCodeRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return qrCodes.stream()
                .map(qr -> {
                    Url url = urlRepository.findByShortCode(qr.getShortCode()).orElse(null);
                    String svg = generateSvgForQr(qr);
                    return QrCodeResponse.fromEntity(qr, url, baseUrl, svg);
                })
                .filter(res -> {
                    // Expiration filter
                    if ("expired".equalsIgnoreCase(qrExpiration)) {
                        if (res.getExpiresAt() == null || res.getExpiresAt().isAfter(Instant.now())) return false;
                    }
                    // Attachment filter
                    if ("with".equalsIgnoreCase(linkAttachment) && !Boolean.TRUE.equals(res.getVisibleAsLink())) {
                        return false;
                    }
                    if ("without".equalsIgnoreCase(linkAttachment) && Boolean.TRUE.equals(res.getVisibleAsLink())) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    @Override
    @Transactional
    public QrCodeResponse getQrCodeDetails(String code, Long userId) {
        QrCode qr = getOrCreateQr(code, userId);
        Url url = urlRepository.findByShortCode(code).orElse(null);
        String svg = generateSvgForQr(qr);
        return QrCodeResponse.fromEntity(qr, url, baseUrl, svg);
    }

    @Override
    public QrImageResult getQrCodeImage(String code, Long userId, String format) {
        QrCode qr = getOrCreateQr(code, userId);

        String cacheKey = QR_CACHE_PREFIX + code + ":" + format;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof byte[] cachedBytes) {
                String ct = "png".equalsIgnoreCase(format) ? "image/png" : "image/svg+xml";
                return new QrImageResult(cachedBytes, ct, true);
            } else if (cached instanceof String cachedStr) {
                return new QrImageResult(cachedStr.getBytes(StandardCharsets.UTF_8), "image/svg+xml", true);
            }
        } catch (Exception ignored) {}

        String targetUrl = baseUrl + "/r/" + code + "?qr=1";
        byte[] data;
        String contentType;

        if ("png".equalsIgnoreCase(format)) {
            data = QrCodeGeneratorUtil.generatePng(
                    targetUrl,
                    qr.getDotsColor(),
                    qr.getBackgroundColor(),
                    qr.getDotsStyle(),
                    qr.getCornersStyle(),
                    qr.getCornersDotStyle(),
                    qr.getCenterText(),
                    qr.getLogoUrl()
            );
            contentType = "image/png";
        } else {
            String svg = generateSvgForQr(qr);
            data = svg.getBytes(StandardCharsets.UTF_8);
            contentType = "image/svg+xml";
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, data, CACHE_TTL);
        } catch (Exception ignored) {}

        return new QrImageResult(data, contentType, false);
    }

    @Override
    @Transactional
    public void deleteQrCode(String code, Long userId) {
        QrCode qr = getQrOrThrow(code);
        verifyQrOwnership(qr, userId);

        Optional<Url> urlOpt = urlRepository.findByShortCode(code);
        if (urlOpt.isPresent()) {
            Url url = urlOpt.get();
            url.setHasQR(false);
            url.setQrCodeId(null);
            urlRepository.save(url);
        }

        qrCodeRepository.delete(qr);
        try {
            Set<String> keys = redisTemplate.keys(QR_CACHE_PREFIX + code + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public Map<String, Object> duplicateQrCode(String sourceCode, String targetShortCode, Long userId) {
        QrCode sourceQr = getQrOrThrow(sourceCode);
        verifyQrOwnership(sourceQr, userId);

        Url targetUrl = urlRepository.findByShortCode(targetShortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Target short URL not found"));
        verifyUrlOwnership(targetUrl, userId);

        QrCode cloned = QrCode.builder()
                .userId(userId)
                .shortCode(targetShortCode)
                .dotsColor(sourceQr.getDotsColor())
                .backgroundColor(sourceQr.getBackgroundColor())
                .dotsStyle(sourceQr.getDotsStyle())
                .cornersStyle(sourceQr.getCornersStyle())
                .cornersDotStyle(sourceQr.getCornersDotStyle())
                .logoUrl(sourceQr.getLogoUrl())
                .centerText(sourceQr.getCenterText())
                .build();

        QrCode savedCloned = qrCodeRepository.save(cloned);
        targetUrl.setHasQR(true);
        targetUrl.setQrCodeId(savedCloned.getId());
        urlRepository.save(targetUrl);

        String redirectUrl = baseUrl + "/r/" + targetShortCode + "?qr=1";
        byte[] pngBytes = QrCodeGeneratorUtil.generatePng(
                redirectUrl,
                cloned.getDotsColor(),
                cloned.getBackgroundColor(),
                cloned.getDotsStyle(),
                cloned.getCornersStyle(),
                cloned.getCornersDotStyle(),
                cloned.getCenterText(),
                cloned.getLogoUrl()
        );

        String svg = generateSvgForQr(savedCloned);
        QrCodeResponse response = QrCodeResponse.fromEntity(savedCloned, targetUrl, baseUrl, svg);

        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", response);
        result.put("imageBase64", QrCodeGeneratorUtil.toBase64DataUrl(pngBytes));
        result.put("svg", svg);
        return result;
    }

    @Override
    @Transactional
    public QrCodeResponse updateQrCode(String code, UpdateQrCodeRequest request, Long userId) {
        QrCode qr = getQrOrThrow(code);
        verifyQrOwnership(qr, userId);

        if (request.getIsHidden() != null) {
            qr.setIsHidden(request.getIsHidden());
        }

        if (request.getQrConfig() != null) {
            Map<String, Object> config = request.getQrConfig();
            if (config.get("dotsColor") instanceof String s && !s.isBlank()) qr.setDotsColor(s.trim());
            if (config.get("backgroundColor") instanceof String s && !s.isBlank()) qr.setBackgroundColor(s.trim());
            if (config.get("dotsStyle") instanceof String s && !s.isBlank()) qr.setDotsStyle(s.trim());
            if (config.get("cornersStyle") instanceof String s && !s.isBlank()) qr.setCornersStyle(s.trim());
            if (config.get("cornersDotStyle") instanceof String s && !s.isBlank()) qr.setCornersDotStyle(s.trim());
            if (config.containsKey("logoUrl")) qr.setLogoUrl((String) config.get("logoUrl"));
            if (config.containsKey("centerText")) qr.setCenterText((String) config.get("centerText"));
        }

        QrCode updated = qrCodeRepository.save(qr);
        evictQrCache(code);

        Url url = urlRepository.findByShortCode(code).orElse(null);
        String svg = generateSvgForQr(updated);
        return QrCodeResponse.fromEntity(updated, url, baseUrl, svg);
    }

    private void evictQrCache(String shortCode) {
        try {
            Set<String> keys = redisTemplate.keys(QR_CACHE_PREFIX + shortCode + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis cache eviction failed for shortCode {}: {}", shortCode, e.getMessage());
        }
    }

    private String generateSvgForQr(QrCode qr) {
        if (qr == null) return null;
        String shortCode = qr.getShortCode();
        String targetUrl = baseUrl + "/r/" + shortCode + "?qr=1";
        String cacheKey = QR_CACHE_PREFIX + shortCode + ":svg";

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof String svgStr && !svgStr.isEmpty()) {
                return svgStr;
            } else if (cached instanceof byte[] bytes && bytes.length > 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for key {}: {}", cacheKey, e.getMessage());
        }

        String svg = QrCodeGeneratorUtil.generateSvg(
                targetUrl,
                qr.getDotsColor(),
                qr.getBackgroundColor(),
                qr.getDotsStyle(),
                qr.getCornersStyle(),
                qr.getCornersDotStyle(),
                qr.getCenterText(),
                qr.getLogoUrl()
        );

        try {
            redisTemplate.opsForValue().set(cacheKey, svg, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis caching failed for key {}: {}", cacheKey, e.getMessage());
        }

        return svg;
    }

    private QrCode getQrOrThrow(String code) {
        return qrCodeRepository.findByShortCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("QR Code not found for code: " + code));
    }

    private QrCode getOrCreateQr(String code, Long userId) {
        Optional<QrCode> qrOpt = qrCodeRepository.findByShortCode(code);
        if (qrOpt.isPresent()) {
            QrCode qr = qrOpt.get();
            verifyQrOwnership(qr, userId);
            return qr;
        }

        // Fallback: check if short URL exists and user owns it
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("QR Code or Short URL not found for code: " + code));
        verifyUrlOwnership(url, userId);

        QrCode defaultQr = QrCode.builder()
                .userId(userId)
                .shortCode(code)
                .build();
        QrCode savedQr = qrCodeRepository.save(defaultQr);
        url.setHasQR(true);
        url.setQrCodeId(savedQr.getId());
        urlRepository.save(url);
        return savedQr;
    }

    private void verifyQrOwnership(QrCode qr, Long userId) {
        if (!qr.getUserId().equals(userId)) {
            throw new ForbiddenException("Forbidden: You do not own this QR Code");
        }
    }

    private void verifyUrlOwnership(Url url, Long userId) {
        if (url.getUserId() != null && !url.getUserId().equals(userId)) {
            throw new ForbiddenException("Forbidden: You do not own this URL");
        }
    }
}
