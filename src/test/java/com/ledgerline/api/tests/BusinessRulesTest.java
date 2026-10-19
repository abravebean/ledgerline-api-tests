package com.ledgerline.api.tests;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("rules")
@DisplayName("Business rules")
class BusinessRulesTest extends ApiTest {

    @Test
    @DisplayName("Daily limit: $2,500.00 in total is allowed, the next cent is refused with the amount left")
    void dailyTransferLimit() {
        TestCustomer customer = BankClient.createTestCustomer(0, 1_000_000);
        BankClient client = BankClient.signedInAs(customer);
        String from = customer.savings().id();
        String to = customer.checking().id();

        assertThat(client.transfer(from, to, "2500.01").statusCode()).isEqualTo(422);
        assertThat(client.transfer(from, to, "1500.00").statusCode()).isEqualTo(201);

        Response over = client.transfer(from, to, "1000.01");
        assertThat(over.statusCode()).isEqualTo(422);
        assertThat(over.jsonPath().getString("error")).contains("You can transfer $1,000.00 more today");

        assertThat(client.transfer(from, to, "1000.00").statusCode()).isEqualTo(201);
        assertThat(client.transfer(from, to, "0.01").statusCode()).isEqualTo(422);
    }

    @Test
    @DisplayName("Source and destination must be different accounts")
    void sameAccountIsRejected() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient client = BankClient.signedInAs(customer);

        Response response = client.transfer(customer.checking().id(), customer.checking().id(), "5.00");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("field")).isEqualTo("toAccountId");
    }

    @Test
    @DisplayName("History is newest first and every running balance adds up")
    void historyRunningBalance() {
        TestCustomer customer = BankClient.createTestCustomer(50_000, 0);
        BankClient client = BankClient.signedInAs(customer);
        for (String amount : List.of("12.34", "100.00", "0.66")) {
            client.transfer(customer.checking().id(), customer.savings().id(), amount).then().statusCode(201);
        }

        List<Map<String, Object>> transactions = client.transactions(customer.checking().id())
                .then().statusCode(200)
                .extract().jsonPath().getList("transactions");

        List<Long> amounts = transactions.stream().map(t -> ((Number) t.get("amountCents")).longValue()).toList();
        assertThat(amounts).containsExactly(-66L, -10_000L, -1_234L, 50_000L);

        // Walk oldest -> newest: each balance is the previous balance plus this entry.
        List<Map<String, Object>> oldestFirst = new java.util.ArrayList<>(transactions);
        Collections.reverse(oldestFirst);
        long running = 0;
        for (Map<String, Object> t : oldestFirst) {
            running += ((Number) t.get("amountCents")).longValue();
            assertThat(((Number) t.get("balanceAfterCents")).longValue()).isEqualTo(running);
        }
        assertThat(client.balanceCents("checking")).isEqualTo(running);
    }
}
