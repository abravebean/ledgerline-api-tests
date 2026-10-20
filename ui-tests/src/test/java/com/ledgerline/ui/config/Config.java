package com.ledgerline.ui.config;

/** Settings from environment variables or -D system properties, with local defaults. */
public final class Config {

    private Config() {
    }

    /** Where Ledgerline runs: LEDGERLINE_BASE_URL, then -Dledgerline.baseUrl, then localhost:3000. */
    public static String baseUrl() {
        String url = firstSet(System.getenv("LEDGERLINE_BASE_URL"), System.getProperty("ledgerline.baseUrl"), "http://localhost:3000");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** chrome (default), firefox or edge. */
    public static String browser() {
        return firstSet(System.getenv("BROWSER"), System.getProperty("browser"), "chrome").toLowerCase();
    }

    /** Headless unless HEADLESS=false or -Dheadless=false, so you can watch a run locally. */
    public static boolean headless() {
        return !"false".equalsIgnoreCase(firstSet(System.getenv("HEADLESS"), System.getProperty("headless"), "true"));
    }

    private static String firstSet(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
