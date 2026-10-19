package com.ledgerline.api.tests;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalence partitioning and boundary value analysis on the transfer amount.
 * Every case starts from a fresh customer with $1,000.00 in checking.
 */
@Tag("rules")
@DisplayName("Transfer amount: partitions and boundaries")
class TransferAmountBoundaryTest extends ApiTest {

    @ParameterizedTest(name = "\"{0}\" -> {1} ({2})")
    @CsvSource(textBlock = """
            0,        400, zero: just below the smallest amount
            0.01,     201, smallest valid amount
            -5.00,    400, negative
            1.234,    400, three decimal places
            ten,      400, not a number
            1e2,      400, scientific notation
            '',       400, empty
            999.99,   201, one cent under the balance
            1000.00,  201, exactly the balance
            1000.01,  422, one cent over the balance
            """)
    void amountIsValidatedAtItsBoundaries(String amount, int expectedStatus, String why) {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 500_000);
        BankClient client = BankClient.signedInAs(customer);

        Response response = client.transfer(customer.checking().id(), customer.savings().id(), amount);

        assertThat(response.statusCode()).as(why).isEqualTo(expectedStatus);
        long expectedChecking = expectedStatus == 201 ? 100_000 - Math.round(Double.parseDouble(amount) * 100) : 100_000;
        assertThat(client.balanceCents("checking")).as("checking balance after: " + why).isEqualTo(expectedChecking);
        if (expectedStatus == 400) {
            assertThat(response.jsonPath().getString("field")).isEqualTo("amount");
        }
    }
}
