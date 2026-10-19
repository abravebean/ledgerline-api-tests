package com.ledgerline.api.tests;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
@DisplayName("Authorization (OWASP A01)")
class AuthorizationTest extends ApiTest {

    @Test
    @DisplayName("A customer can't read another customer's transactions (IDOR)")
    void cannotReadSomeoneElsesAccount() {
        TestCustomer alice = BankClient.createTestCustomer(100_000, 0);
        TestCustomer mallory = BankClient.createTestCustomer(100_000, 0);

        Response response = BankClient.signedInAs(mallory).transactions(alice.checking().id());

        // 404, not 403: the API doesn't even confirm the account exists.
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.asString()).doesNotContain(alice.checking().balance());
    }

    @Test
    @DisplayName("A customer can't move money out of someone else's account")
    void cannotTransferFromSomeoneElsesAccount() {
        TestCustomer alice = BankClient.createTestCustomer(100_000, 0);
        TestCustomer mallory = BankClient.createTestCustomer(0, 0);

        Response response = BankClient.signedInAs(mallory)
                .transfer(alice.checking().id(), mallory.savings().id(), "500.00");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(BankClient.signedInAs(alice).balanceCents("checking")).isEqualTo(100_000);
    }

    @ParameterizedTest(name = "{0} {1} needs a session")
    @CsvSource({"GET, /me", "GET, /accounts", "POST, /transfers"})
    void accountRoutesRequireASession(String method, String path) {
        Response response = new BankClient().request().body("{}").request(method, path);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Signing out ends the session on the server, so a copied cookie stops working")
    void logoutInvalidatesTheSession() {
        BankClient client = BankClient.signedInAs(BankClient.createTestCustomer(100_000, 0));
        String token = client.sessionToken();
        assertThat(BankClient.withSessionToken(token).get("/accounts").statusCode()).isEqualTo(200);

        client.logout().then().statusCode(204);

        assertThat(BankClient.withSessionToken(token).get("/accounts").statusCode()).isEqualTo(401);
    }
}
