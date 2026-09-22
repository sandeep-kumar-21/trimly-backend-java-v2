package com.trimly.api.service;

import java.util.Map;

public interface RedirectService {

    record RedirectResult(String targetUrl, boolean isPasswordProtected, String redirectUrl) {}

    RedirectResult resolveRedirect(
            String code,
            String clientIp,
            String referrer,
            String userAgent,
            boolean isQrScan,
            Map<String, String> utms
    );
}
