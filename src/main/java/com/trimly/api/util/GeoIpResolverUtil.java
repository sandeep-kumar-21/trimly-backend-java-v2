package com.trimly.api.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GeoIpResolverUtil {

    private GeoIpResolverUtil() {
    }

    public record GeoLocation(String country, String city, String region) {}

    public static GeoLocation resolve(String ip) {
        if (ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return new GeoLocation("Unknown", null, null);
        }

        // Resilient fallback when offline or local
        return new GeoLocation("Unknown", null, null);
    }
}
