package com.trimly.api.service;

import java.util.Map;

public interface AsyncClickService {

    void recordClickAsync(
            String shortCode,
            Long userId,
            Long campaignId,
            String reqIp,
            String referrer,
            String userAgent,
            boolean isQrScan,
            Map<String, String> utms
    );
}
