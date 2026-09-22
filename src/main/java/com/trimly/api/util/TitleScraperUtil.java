package com.trimly.api.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public final class TitleScraperUtil {

    private static final int TIMEOUT_MS = 3000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 TrimlyBot/1.0";

    private TitleScraperUtil() {
    }

    public static String scrapeTitle(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            String title = doc.title();
            if (title != null && !title.trim().isEmpty()) {
                String trimmed = title.trim();
                return trimmed.length() > 250 ? trimmed.substring(0, 247) + "..." : trimmed;
            }
        } catch (Exception ex) {
            log.debug("Could not scrape title for URL {}: {}", url, ex.getMessage());
        }

        return null;
    }
}
