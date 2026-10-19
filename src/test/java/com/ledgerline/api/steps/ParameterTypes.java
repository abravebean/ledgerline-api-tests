package com.ledgerline.api.steps;

import com.ledgerline.api.support.Money;
import io.cucumber.java.ParameterType;

public class ParameterTypes {

    /** {money} matches $1,000.00 or -$250.00 and turns it into cents. */
    @ParameterType("-?\\$[\\d,]+\\.\\d{2}")
    public Long money(String text) {
        boolean negative = text.startsWith("-");
        long cents = Money.toCents(text.replace("-", "").replace("$", "").replace(",", ""));
        return negative ? -cents : cents;
    }

    /** {account} is checking or savings. */
    @ParameterType("checking|savings")
    public String account(String type) {
        return type;
    }
}
