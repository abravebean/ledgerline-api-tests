package com.ledgerline.api.tests;

import com.ledgerline.api.support.LedgerlineAvailable;
import org.junit.jupiter.api.BeforeAll;

/** Base class: every API test class first checks that Ledgerline is up with its test hooks on. */
abstract class ApiTest {

    @BeforeAll
    static void ledgerlineIsRunning() {
        LedgerlineAvailable.check();
    }
}
