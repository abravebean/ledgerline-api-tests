package com.ledgerline.api.support;

import java.math.BigDecimal;

/** Converts between display amounts ("$1,250.00") and integer cents, without floating point. */
public final class Money {

    private Money() {
    }

    /** "$1,250.00" or "1250" -> 125000 */
    public static long toCents(String amount) {
        String plain = amount.replace("$", "").replace(",", "").trim();
        return new BigDecimal(plain).movePointRight(2).longValueExact();
    }

    /** 125000 -> "1250.00", the format the API accepts */
    public static String plain(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2).toPlainString();
    }

    /** 125000 -> "$1,250.00" */
    public static String format(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return String.format("%s$%,d.%02d", sign, abs / 100, abs % 100);
    }
}
