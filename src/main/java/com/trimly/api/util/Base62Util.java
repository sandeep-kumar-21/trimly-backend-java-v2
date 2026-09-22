package com.trimly.api.util;

/**
 * High-performance, thread-safe utility for Base62 encoding and decoding.
 * Used for generating collision-free short URL codes from database sequence values.
 */
public final class Base62Util {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_CHARS.length(); // 62

    private Base62Util() {
        // Prevent instantiation
    }

    /**
     * Encodes a positive 64-bit integer into a Base62 string.
     *
     * @param num positive number (e.g., from PostgreSQL url_code_seq)
     * @return Base62 encoded string representation
     */
    public static String encode(long num) {
        if (num < 0) {
            throw new IllegalArgumentException("Number to encode must be non-negative: " + num);
        }
        if (num == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        long current = num;
        while (current > 0) {
            int remainder = (int) (current % BASE);
            sb.append(BASE62_CHARS.charAt(remainder));
            current /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back into its original positive 64-bit integer.
     *
     * @param str Base62 string
     * @return decoded long number
     */
    public static long decode(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Base62 string cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int index = BASE62_CHARS.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + index;
        }

        return result;
    }
}
