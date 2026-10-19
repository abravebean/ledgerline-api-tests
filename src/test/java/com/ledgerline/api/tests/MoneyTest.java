package com.ledgerline.api.tests;

import com.ledgerline.api.support.Money;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the test helper itself. No server needed. */
@Tag("unit")
class MoneyTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            0.01      | 1      | $0.01      | 0.01
            1250      | 125000 | $1,250.00  | 1250.00
            '$1,000.00' | 100000 | $1,000.00 | 1000.00
            """)
    void convertsWithoutFloatingPoint(String input, long cents, String display, String plain) {
        assertThat(Money.toCents(input)).isEqualTo(cents);
        assertThat(Money.format(cents)).isEqualTo(display);
        assertThat(Money.plain(cents)).isEqualTo(plain);
    }
}
