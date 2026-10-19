package com.ledgerline.api.support;

import com.ledgerline.api.config.Config;
import io.restassured.RestAssured;

/** Fails fast, with instructions, when Ledgerline isn't running or its test hooks are off. */
public final class LedgerlineAvailable {

    private static volatile boolean checked;

    private LedgerlineAvailable() {
    }

    public static synchronized void check() {
        if (checked) {
            return;
        }
        String base = Config.baseUrl();
        int health;
        try {
            health = RestAssured.given().baseUri(base).get("/api/health").statusCode();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Ledgerline is not reachable at " + base + ". Start it from the Bank-automation-test-bench repo with:\n"
                            + "  DEMO_BANK_TEST_HOOKS=true npm start\n"
                            + "or point the tests elsewhere with LEDGERLINE_BASE_URL.", e);
        }
        if (health != 200) {
            throw new IllegalStateException("Ledgerline health check at " + base + "/api/health returned " + health);
        }
        int hooks = RestAssured.given().baseUri(base).get("/bank/api/test/integrity").statusCode();
        if (hooks != 200) {
            throw new IllegalStateException(
                    "Ledgerline is running but its test hooks are off. Restart it with DEMO_BANK_TEST_HOOKS=true.");
        }
        checked = true;
    }
}
