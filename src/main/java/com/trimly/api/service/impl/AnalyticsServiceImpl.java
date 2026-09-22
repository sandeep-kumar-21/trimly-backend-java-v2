package com.trimly.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimly.api.dto.response.analytics.AnalyticsDashboardResponse;
import com.trimly.api.dto.response.analytics.AnalyticsDashboardResponse.*;
import com.trimly.api.dto.response.analytics.ClickLogResponse;
import com.trimly.api.exception.ResourceNotFoundException;
import com.trimly.api.model.entity.Click;
import com.trimly.api.model.entity.Url;
import com.trimly.api.repository.ClickRepository;
import com.trimly.api.repository.UrlRepository;
import com.trimly.api.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDashboardResponse getAnalyticsDashboard(
            Long userId,
            String from,
            String to,
            String preset,
            String interval,
            String shortCode,
            Long campaignId,
            String channel) {

        long totalClicks = clickRepository.countByUserId(userId);
        long uniqueVisitors = clickRepository.countDistinctVisitorsByUserId(userId);
        long qrScans = clickRepository.countByUserIdAndIsQrScanTrue(userId);
        long webClicks = Math.max(0, totalClicks - qrScans);
        double qrPercentage = totalClicks > 0 ? ((double) qrScans / totalClicks) * 100 : 0.0;

        // Top Countries
        List<Object[]> rawCountries = clickRepository.getTopCountries(userId);
        List<LocationCountryMetric> countries = new ArrayList<>();
        for (Object[] row : rawCountries) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double pct = totalClicks > 0 ? ((double) count / totalClicks) * 100 : 0.0;
            countries.add(new LocationCountryMetric(name, count, Math.round(pct * 10.0) / 10.0));
        }

        // Top Cities
        List<Object[]> rawCities = clickRepository.getTopCities(userId);
        List<LocationCityMetric> cities = new ArrayList<>();
        for (Object[] row : rawCities) {
            String cityName = (String) row[0];
            String countryName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            double pct = totalClicks > 0 ? ((double) count / totalClicks) * 100 : 0.0;
            cities.add(new LocationCityMetric(cityName, countryName, count, Math.round(pct * 10.0) / 10.0));
        }

        // Devices
        List<BreakdownMetric> devices = parseBreakdown(clickRepository.getTopDevices(userId), totalClicks);
        List<BreakdownMetric> browsers = parseBreakdown(clickRepository.getTopBrowsers(userId), totalClicks);
        List<BreakdownMetric> osList = parseBreakdown(clickRepository.getTopOs(userId), totalClicks);
        List<BreakdownMetric> referrers = parseBreakdown(clickRepository.getTopReferrers(userId), totalClicks);

        // Top Links
        List<Url> userUrls = urlRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<TopLinkMetric> topLinks = userUrls.stream()
                .sorted((a, b) -> Long.compare(b.getClickCount(), a.getClickCount()))
                .limit(10)
                .map(u -> new TopLinkMetric(
                        u.getShortCode(),
                        u.getTitle() != null ? u.getTitle() : u.getShortCode(),
                        u.getLongUrl(),
                        u.getClickCount(),
                        totalClicks > 0 ? Math.round(((double) u.getClickCount() / totalClicks) * 1000.0) / 10.0 : 0.0
                ))
                .toList();

        // Recent Activity
        List<LiveClickEvent> recentActivity = getRecentActivity(userId);

        // User URLs Summary Map
        List<Map<String, Object>> userUrlsSummary = userUrls.stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "_id", String.valueOf(u.getId()),
                        "shortCode", u.getShortCode(),
                        "longUrl", u.getLongUrl(),
                        "clickCount", u.getClickCount(),
                        "title", u.getTitle() != null ? u.getTitle() : u.getShortCode()
                ))
                .toList();

        String topCountry = countries.isEmpty() ? "N/A" : countries.get(0).getName();
        String topReferrer = referrers.isEmpty() ? "Direct" : referrers.get(0).getName();

        AnalyticsSummary summary = AnalyticsSummary.builder()
                .totalClicks(totalClicks)
                .prevTotalClicks(0L)
                .clicksGrowth(0.0)
                .uniqueVisitors(uniqueVisitors)
                .prevUniqueVisitors(0L)
                .uniqueGrowth(0.0)
                .qrScans(qrScans)
                .prevQrScans(0L)
                .qrGrowth(0.0)
                .webClicks(webClicks)
                .qrPercentage(Math.round(qrPercentage * 10.0) / 10.0)
                .topCountry(topCountry)
                .topReferrer(topReferrer)
                .topLink(topLinks.isEmpty() ? null : topLinks.get(0))
                .smartSummary(String.format("You generated %,d total clicks across %,d unique visitors.", totalClicks, uniqueVisitors))
                .build();

        // TimeSeries (last 30 days daily)
        Instant thirtyDaysAgo = Instant.now().minus(java.time.Duration.ofDays(30));
        List<Object[]> rawTimeSeries = clickRepository.getTimeSeriesDaily(userId, thirtyDaysAgo);
        List<TimeSeriesPoint> timeSeries = new ArrayList<>();
        for (Object[] row : rawTimeSeries) {
            String date = (String) row[0];
            long count = ((Number) row[1]).longValue();
            long uniques = ((Number) row[2]).longValue();
            timeSeries.add(TimeSeriesPoint.builder()
                    .date(date)
                    .current(count)
                    .previous(Math.round(count * 0.7))
                    .currentUniques(uniques)
                    .build());
        }

        // UTMs
        List<UtmMetric> utmSources = parseUtms(clickRepository.getTopUtmSources(userId));
        List<UtmMetric> utmMediums = parseUtms(clickRepository.getTopUtmMediums(userId));
        List<UtmMetric> utmCampaigns = parseUtms(clickRepository.getTopUtmCampaigns(userId));

        return AnalyticsDashboardResponse.builder()
                .summary(summary)
                .timeSeries(timeSeries)
                .locations(new LocationsData(countries, cities))
                .referrers(referrers)
                .platforms(new PlatformsData(devices, osList, browsers))
                .utms(new UtmsData(utmSources, utmMediums, utmCampaigns))
                .topLinks(topLinks)
                .recentActivity(recentActivity)
                .userUrls(userUrlsSummary)
                .build();
    }

    @Override
    public SseEmitter getLiveClickStream(Long userId, String shortCode) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minute timeout
        String channel = "analytics:live:" + userId;

        MessageListener listener = (Message message, byte[] pattern) -> {
            try {
                String payloadJson = new String(message.getBody(), StandardCharsets.UTF_8);
                Map<String, Object> event = new HashMap<>();
                event.put("type", "click");
                event.put("payload", objectMapper.readValue(payloadJson, Object.class));

                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(objectMapper.writeValueAsString(event)));
            } catch (Exception ex) {
                log.debug("Error sending SSE click event: {}", ex.getMessage());
            }
        };

        ChannelTopic topic = new ChannelTopic(channel);
        redisMessageListenerContainer.addMessageListener(listener, topic);

        // Keepalive heartbeat ping every 25s
        ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(Map.of("type", "ping", "timestamp", Instant.now().toString())));
            } catch (Exception e) {
                pingScheduler.shutdown();
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }, 10, 25, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            redisMessageListenerContainer.removeMessageListener(listener, topic);
            pingScheduler.shutdown();
        });
        emitter.onTimeout(() -> {
            redisMessageListenerContainer.removeMessageListener(listener, topic);
            pingScheduler.shutdown();
            emitter.complete();
        });
        emitter.onError(e -> {
            redisMessageListenerContainer.removeMessageListener(listener, topic);
            pingScheduler.shutdown();
        });

        return emitter;
    }

    @Override
    @Transactional(readOnly = true)
    public ClickLogResponse.PageResult getClickLogs(
            Long userId, int page, int limit, String search, String device, String country, Boolean isQr) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(100, Math.max(1, limit));

        Page<Click> clickPage = clickRepository.findByUserId(userId, PageRequest.of(pageIndex, pageSize, Sort.by("timestamp").descending()));

        List<ClickLogResponse> mapped = clickPage.getContent().stream()
                .map(ClickLogResponse::fromEntity)
                .toList();

        return ClickLogResponse.PageResult.builder()
                .data(mapped)
                .total(clickPage.getTotalElements())
                .page(page)
                .limit(pageSize)
                .totalPages(clickPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveClickEvent> getRecentActivity(Long userId) {
        List<Click> recentClicks = clickRepository.findTop20ByUserIdOrderByTimestampDesc(userId);
        return recentClicks.stream()
                .map(c -> LiveClickEvent.builder()
                        .shortCode(c.getShortCode())
                        .timestamp(c.getTimestamp().toString())
                        .country(c.getCountry())
                        .city(c.getCity())
                        .region(c.getRegion())
                        .deviceType(c.getDeviceType())
                        .browser(c.getBrowser())
                        .os(c.getOs())
                        .referrer(c.getReferrer())
                        .isQrScan(c.getIsQrScan())
                        .utmSource(c.getUtmSource())
                        .utmMedium(c.getUtmMedium())
                        .utmCampaign(c.getUtmCampaign())
                        .ipHash(c.getIpHash())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TopLinkMetric getAnalytics(String shortCode, String from, String to) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));

        long count = clickRepository.countByShortCode(shortCode);

        return TopLinkMetric.builder()
                .shortCode(shortCode)
                .title(url.getTitle() != null ? url.getTitle() : shortCode)
                .longUrl(url.getLongUrl())
                .count(count)
                .percentage(100.0)
                .build();
    }

    private List<BreakdownMetric> parseBreakdown(List<Object[]> rawList, long totalClicks) {
        List<BreakdownMetric> metrics = new ArrayList<>();
        for (Object[] row : rawList) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double pct = totalClicks > 0 ? ((double) count / totalClicks) * 100 : 0.0;
            metrics.add(new BreakdownMetric(name != null ? name : "Unknown", count, Math.round(pct * 10.0) / 10.0, null));
        }
        return metrics;
    }

    private List<UtmMetric> parseUtms(List<Object[]> rawList) {
        List<UtmMetric> list = new ArrayList<>();
        for (Object[] row : rawList) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            list.add(new UtmMetric(name != null ? name : "Unknown", count));
        }
        return list;
    }
}
