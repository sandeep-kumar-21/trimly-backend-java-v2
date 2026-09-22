package com.trimly.api.controller;

import com.trimly.api.dto.response.analytics.AnalyticsDashboardResponse;
import com.trimly.api.dto.response.analytics.ClickLogResponse;
import com.trimly.api.exception.UnauthorizedException;
import com.trimly.api.security.SecurityUtils;
import com.trimly.api.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics dashboard, logs, and real-time SSE click telemetry")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get unified aggregate analytics dashboard across all URLs")
    @ApiResponse(responseCode = "200", description = "Returns complete analytics dashboard payload.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    public ResponseEntity<AnalyticsDashboardResponse> getDashboard(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String preset,
            @RequestParam(required = false, defaultValue = "daily") String interval,
            @RequestParam(required = false) String shortCode,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) String channel) {
        Long userId = getCurrentUserId();
        AnalyticsDashboardResponse response = analyticsService.getAnalyticsDashboard(
                userId, from, to, preset, interval, shortCode, campaignId, channel);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream real-time click telemetry via Server-Sent Events (SSE) backed by Redis Pub/Sub")
    public SseEmitter streamLiveClicks(@RequestParam(required = false) String shortCode) {
        Long userId = getCurrentUserId();
        return analyticsService.getLiveClickStream(userId, shortCode);
    }

    @GetMapping("/logs")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get paginated detailed click audit log")
    @ApiResponse(responseCode = "200", description = "Returns paginated click logs.")
    public ResponseEntity<ClickLogResponse.PageResult> getClickLogs(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean isQr) {
        Long userId = getCurrentUserId();
        ClickLogResponse.PageResult response = analyticsService.getClickLogs(userId, page, limit, search, device, country, isQr);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get latest 20 real-time clicks for user short links")
    @ApiResponse(responseCode = "200", description = "Returns recent click events.")
    public ResponseEntity<List<AnalyticsDashboardResponse.LiveClickEvent>> getRecentActivity() {
        Long userId = getCurrentUserId();
        List<AnalyticsDashboardResponse.LiveClickEvent> response = analyticsService.getRecentActivity(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get analytics metrics for a specific short URL")
    @ApiResponse(responseCode = "200", description = "Returns analytics metrics.")
    @ApiResponse(responseCode = "404", description = "Short URL not found.")
    public ResponseEntity<AnalyticsDashboardResponse.TopLinkMetric> getAnalytics(
            @PathVariable String code,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        AnalyticsDashboardResponse.TopLinkMetric response = analyticsService.getAnalytics(code, from, to);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized: Login required"));
    }
}
