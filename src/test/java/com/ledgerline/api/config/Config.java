package com.ledgerline.api.config;

/**
 * Where the tests find Ledgerline.
 * Precedence: LEDGERLINE_BASE_URL environment variable, then the ledgerline.baseUrl
 * system property (set in pom.xml, override with -Dledgerline.baseUrl=...), then localhost:3000.
 */
public final class Config {

    private static final String DEFAULT_BASE_URL = "http://localhost:3000";

    private Config() {
    }

    public static String baseUrl() {
        String fromEnv = System.getenv("LEDGERLINE_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return stripSlash(fromEnv);
        }
        return stripSlash(System.getProperty("ledgerline.baseUrl", DEFAULT_BASE_URL));
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
