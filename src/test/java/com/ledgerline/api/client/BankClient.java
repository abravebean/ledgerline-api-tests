package com.ledgerline.api.client;

import com.ledgerline.api.config.Config;
import com.ledgerline.api.model.Account;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * A thin client for the Ledgerline REST API (/bank/api).
 *
 * Each instance holds one customer's session token and sends it as the ll_session cookie,
 * so two clients are two independent customers. Holding the token (instead of a shared
 * cookie store) also makes a client safe to use from many threads at once, which the
 * concurrency tests rely on.
 */
public class BankClient {

    public static final String SESSION_COOKIE = "ll_session";

    private final RequestSpecification spec;
    private volatile String sessionToken;

    public BankClient() {
        this.spec = new RequestSpecBuilder()
                .setBaseUri(Config.baseUrl())
                .setBasePath("/bank/api")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    // ------------------------------------------------------------------ test setup

    /** Creates a fresh customer through the test-only route (needs DEMO_BANK_TEST_HOOKS=true on the server). */
    public static TestCustomer createTestCustomer(long checkingCents, long savingsCents) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "API Test Customer");
        body.put("checkingCents", checkingCents);
        body.put("savingsCents", savingsCents);
        return new BankClient().request()
                .body(body)
                .post("/test/customers")
                .then()
                .log().ifValidationFails(LogDetail.ALL)
                .statusCode(201)
                .extract()
                .as(TestCustomer.class);
    }

    /** A client already signed in as the customer. */
    public static BankClient signedInAs(TestCustomer customer) {
        BankClient client = new BankClient();
        client.login(customer.username(), customer.password())
                .then()
                .log().ifValidationFails(LogDetail.ALL)
                .statusCode(200);
        return client;
    }

    // ------------------------------------------------------------------ API calls

    public Response login(String username, String password) {
        Response response = request()
                .body(Map.of("username", username, "password", password))
                .post("/login");
        String token = response.getCookie(SESSION_COOKIE);
        if (response.statusCode() == 200 && token != null) {
            sessionToken = token;
        }
        return response;
    }

    public Response logout() {
        Response response = request().post("/logout");
        sessionToken = null;
        return response;
    }

    public Response me() {
        return request().get("/me");
    }

    public Response accounts() {
        return request().get("/accounts");
    }

    public Response transactions(String accountId) {
        return request().get("/accounts/{id}/transactions", accountId);
    }

    public Response transfer(String fromAccountId, String toAccountId, String amount) {
        return transfer(fromAccountId, toAccountId, amount, null);
    }

    public Response transfer(String fromAccountId, String toAccountId, String amount, String idempotencyKey) {
        RequestSpecification req = request()
                .body(Map.of("fromAccountId", fromAccountId, "toAccountId", toAccountId, "amount", amount));
        if (idempotencyKey != null) {
            req.header("Idempotency-Key", idempotencyKey);
        }
        return req.post("/transfers");
    }

    // ------------------------------------------------------------------ helpers

    /** Current balance in cents of this customer's account of the given type ("checking" or "savings"). */
    public long balanceCents(String type) {
        List<Account> accounts = accounts()
                .then().statusCode(200)
                .extract().jsonPath().getList("accounts", Account.class);
        return accounts.stream()
                .filter(a -> a.type().equals(type))
                .findFirst()
                .orElseThrow()
                .balanceCents();
    }

    public String sessionToken() {
        return sessionToken;
    }

    /** A request with this client's session cookie, when signed in. */
    public RequestSpecification request() {
        RequestSpecification req = given().spec(spec);
        String token = sessionToken;
        if (token != null) {
            req.cookie(SESSION_COOKIE, token);
        }
        return req;
    }

    /** A request that replays a given session token, e.g. one that should no longer work. */
    public static RequestSpecification withSessionToken(String token) {
        return given().spec(new BankClient().spec).cookie(SESSION_COOKIE, token);
    }
}
