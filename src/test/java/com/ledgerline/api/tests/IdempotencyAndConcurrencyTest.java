package com.ledgerline.api.tests;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integrity")
@DisplayName("Double submits and race conditions (CWE-362)")
class IdempotencyAndConcurrencyTest extends ApiTest {

    @Test
    @DisplayName("The same Idempotency-Key moves money once and replays the first response")
    void idempotentReplay() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient client = BankClient.signedInAs(customer);
        String key = "key-" + UUID.randomUUID();

        Response first = client.transfer(customer.checking().id(), customer.savings().id(), "75.00", key);
        Response second = client.transfer(customer.checking().id(), customer.savings().id(), "75.00", key);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.getHeader("Idempotent-Replay")).isEqualTo("true");
        assertThat(second.jsonPath().getString("transfer.id")).isEqualTo(first.jsonPath().getString("transfer.id"));
        assertThat(client.balanceCents("checking")).isEqualTo(100_000 - 7_500);
    }

    @Test
    @DisplayName("Reusing a key for a different transfer is 409; a malformed key is 400")
    void idempotencyKeyMisuse() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient client = BankClient.signedInAs(customer);
        String from = customer.checking().id();
        String to = customer.savings().id();
        String key = "key-" + UUID.randomUUID();

        assertThat(client.transfer(from, to, "10.00", key).statusCode()).isEqualTo(201);
        assertThat(client.transfer(from, to, "11.00", key).statusCode()).isEqualTo(409);
        assertThat(client.transfer(from, to, "10.00", "bad key!").statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("20 simultaneous $100 transfers from $1,000: exactly 10 succeed, never overdrawn")
    void concurrentTransfersCannotOverdraw() throws Exception {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient client = BankClient.signedInAs(customer);
        int requests = 20;

        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch startGate = new CountDownLatch(1); // release every request at the same moment
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                startGate.await();
                return client.transfer(customer.checking().id(), customer.savings().id(), "100.00").statusCode();
            }));
        }
        startGate.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(10);
        assertThat(statuses).filteredOn(s -> s == 422).hasSize(10);
        assertThat(client.balanceCents("checking")).isZero();
        assertThat(client.balanceCents("savings")).isEqualTo(100_000);
    }
}
