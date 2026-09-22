package com.trimly.api.service;

import com.trimly.api.dto.response.analytics.AnalyticsDashboardResponse;
import com.trimly.api.dto.response.analytics.ClickLogResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AnalyticsService {

    AnalyticsDashboardResponse getAnalyticsDashboard(
            Long userId,
            String from,
            String to,
            String preset,
            String interval,
            String shortCode,
            Long campaignId,
            String channel
    );

    SseEmitter getLiveClickStream(Long userId, String shortCode);

    ClickLogResponse.PageResult getClickLogs(
            Long userId,
            int page,
            int limit,
            String search,
            String device,
            String country,
            Boolean isQr
    );

    List<AnalyticsDashboardResponse.LiveClickEvent> getRecentActivity(Long userId);

    AnalyticsDashboardResponse.TopLinkMetric getAnalytics(String shortCode, String from, String to);
}
