package com.ledgerline.api.tests;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
@DisplayName("Sign-in security (OWASP A07)")
class SignInSecurityTest extends ApiTest {

    @Test
    @DisplayName("Locks after 3 failed attempts and refuses the right password while locked")
    void lockoutAfterThreeFailures() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient client = new BankClient();

        assertThat(client.login(customer.username(), "wrong-1").statusCode()).isEqualTo(401);
        assertThat(client.login(customer.username(), "wrong-2").statusCode()).isEqualTo(401);
        assertThat(client.login(customer.username(), "wrong-3").statusCode()).isEqualTo(423);

        Response locked = client.login(customer.username(), customer.password());
        assertThat(locked.statusCode()).isEqualTo(423);
        assertThat(locked.jsonPath().getString("error")).contains("Try again in 15 minutes");
    }

    @Test
    @DisplayName("Responses don't reveal whether a username exists")
    void noUsernameEnumeration() {
        TestCustomer real = BankClient.createTestCustomer(100_000, 0);
        String ghost = "ghost_" + System.nanoTime();

        assertThat(threeFailedAttempts(ghost)).isEqualTo(threeFailedAttempts(real.username()));
    }

    @Test
    @DisplayName("Session cookie is HttpOnly, SameSite=Strict and scoped to /bank")
    void sessionCookieFlags() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);

        String setCookie = new BankClient().login(customer.username(), customer.password()).getHeader("Set-Cookie");

        assertThat(setCookie)
                .startsWith(BankClient.SESSION_COOKIE + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/bank");
    }

    private static List<String> threeFailedAttempts(String username) {
        BankClient client = new BankClient();
        List<String> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Response r = client.login(username, "wrong");
            results.add(r.statusCode() + " " + r.jsonPath().getString("error"));
        }
        return results;
    }
}
