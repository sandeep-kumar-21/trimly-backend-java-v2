package com.trimly.api.util;

import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

@Slf4j
public final class UserAgentParserUtil {

    private static final UserAgentAnalyzer ANALYZER;

    static {
        ANALYZER = UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(10000)
                .withField(UserAgent.DEVICE_CLASS)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .build();
    }

    private UserAgentParserUtil() {
    }

    public record ParsedUserAgent(String deviceType, String browser, String os) {}

    public static ParsedUserAgent parse(String userAgentString) {
        if (userAgentString == null || userAgentString.trim().isEmpty()) {
            return new ParsedUserAgent("Desktop", "Unknown", "Unknown");
        }

        try {
            UserAgent parsed = ANALYZER.parse(userAgentString);
            String deviceClass = parsed.getValue(UserAgent.DEVICE_CLASS);
            String browser = parsed.getValue(UserAgent.AGENT_NAME);
            String os = parsed.getValue(UserAgent.OPERATING_SYSTEM_NAME);

            String normalizedDevice = switch (deviceClass != null ? deviceClass.toLowerCase() : "") {
                case "phone", "mobile" -> "Mobile";
                case "tablet" -> "Tablet";
                default -> "Desktop";
            };

            return new ParsedUserAgent(
                    normalizedDevice,
                    browser != null && !browser.equals("Unknown") ? browser : "Unknown",
                    os != null && !os.equals("Unknown") ? os : "Unknown"
            );
        } catch (Exception ex) {
            log.debug("Error parsing User-Agent: {}", ex.getMessage());
            return new ParsedUserAgent("Desktop", "Unknown", "Unknown");
        }
    }
}
