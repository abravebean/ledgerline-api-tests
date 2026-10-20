package com.ledgerline.ui.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.ui.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Creates a fresh customer for each test through Ledgerline's test-only API
 * (on only when the server runs with DEMO_BANK_TEST_HOOKS=true). Tests never share
 * balances, daily limits or lockout counters, which is what makes parallel runs safe.
 */
public final class TestCustomers {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private TestCustomers() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(String id, String type, String name, String number, long balanceCents, String balance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(String username, String password, String name, List<Account> accounts) {

        public Account checking() {
            return byType("checking");
        }

        public Account savings() {
            return byType("savings");
        }

        public String firstName() {
            return name.split(" ")[0];
        }

        private Account byType(String type) {
            return accounts.stream().filter(a -> a.type().equals(type)).findFirst().orElseThrow();
        }
    }

    public static Customer create(String name, long checkingCents, long savingsCents) {
        try {
            String body = JSON.writeValueAsString(Map.of("name", name, "checkingCents", checkingCents, "savingsCents", savingsCents));
            HttpRequest request = HttpRequest.newBuilder(URI.create(Config.baseUrl() + "/bank/api/test/customers"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("Could not create a test customer (HTTP " + response.statusCode() + "). "
                        + "Start Ledgerline with its test hooks on: DEMO_BANK_TEST_HOOKS=true npm start");
            }
            return JSON.readValue(response.body(), Customer.class);
        } catch (IOException e) {
            throw new IllegalStateException("Ledgerline isn't reachable at " + Config.baseUrl()
                    + ". Start it with: DEMO_BANK_TEST_HOOKS=true npm start", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
