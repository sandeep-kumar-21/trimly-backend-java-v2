package com.trimly.api.controller;

import com.trimly.api.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "High-speed URL redirection endpoints")
public class RedirectController {

    private final RedirectService redirectService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping({"/r/{code}", "/api/r/{code}", "/{code:[a-zA-Z0-9_-]+}"})
    @Operation(summary = "Redirect short code to original destination URL (HTTP 302)")
    @ApiResponse(responseCode = "302", description = "Redirecting to target URL.")
    @ApiResponse(responseCode = "404", description = "Short code not found or expired.")
    public void handleRedirect(
            @PathVariable String code,
            @RequestParam(required = false) String qr,
            @RequestParam(required = false) String scan,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, name = "utm_source") String utmSource,
            @RequestParam(required = false, name = "utm_medium") String utmMedium,
            @RequestParam(required = false, name = "utm_campaign") String utmCampaign,
            @RequestParam(required = false, name = "utm_term") String utmTerm,
            @RequestParam(required = false, name = "utm_content") String utmContent,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Ignore static and reserved paths
        if ("api".equalsIgnoreCase(code) || "swagger-ui".equalsIgnoreCase(code) || "favicon".equalsIgnoreCase(code)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = (xForwardedFor != null && !xForwardedFor.isEmpty())
                ? xForwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();

        String referrer = request.getHeader("Referer");
        if (referrer == null) {
            referrer = request.getHeader("Referrer");
        }
        String userAgent = request.getHeader("User-Agent");

        boolean isQrScan = "1".equals(qr) || "1".equals(scan) || "qr".equalsIgnoreCase(source);

        Map<String, String> utms = new HashMap<>();
        if (utmSource != null) utms.put("utmSource", utmSource);
        if (utmMedium != null) utms.put("utmMedium", utmMedium);
        if (utmCampaign != null) utms.put("utmCampaign", utmCampaign);
        if (utmTerm != null) utms.put("utmTerm", utmTerm);
        if (utmContent != null) utms.put("utmContent", utmContent);

        RedirectService.RedirectResult result = redirectService.resolveRedirect(
                code,
                clientIp,
                referrer,
                userAgent,
                isQrScan,
                utms
        );

        if (result.isPasswordProtected()) {
            response.sendRedirect(result.redirectUrl());
        } else {
            response.sendRedirect(result.targetUrl());
        }
    }

    @GetMapping("/s/qrc_preview.html")
    public void previewHtml(HttpServletResponse response) throws IOException {
        response.sendRedirect(frontendUrl + "/s/qrc_preview.html");
    }
}
