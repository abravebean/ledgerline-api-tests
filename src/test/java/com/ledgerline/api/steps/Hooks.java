package com.ledgerline.api.steps;

import com.ledgerline.api.support.LedgerlineAvailable;
import io.cucumber.java.BeforeAll;

public class Hooks {

    @BeforeAll
    public static void ledgerlineIsRunning() {
        LedgerlineAvailable.check();
    }
}
