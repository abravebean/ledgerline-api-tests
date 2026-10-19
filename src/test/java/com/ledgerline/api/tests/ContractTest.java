package com.ledgerline.api.tests;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/** Response shapes are checked against JSON Schemas in src/test/resources/schemas. */
@Tag("contract")
@DisplayName("API contracts (JSON Schema)")
class ContractTest extends ApiTest {

    @Test
    @DisplayName("GET /accounts")
    void accounts() {
        BankClient.signedInAs(BankClient.createTestCustomer(100_000, 500_000))
                .accounts()
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/accounts.json"));
    }

    @Test
    @DisplayName("GET /accounts/{id}/transactions")
    void transactions() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient.signedInAs(customer)
                .transactions(customer.checking().id())
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/transactions.json"));
    }

    @Test
    @DisplayName("POST /transfers (201)")
    void transferCreated() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient.signedInAs(customer)
                .transfer(customer.checking().id(), customer.savings().id(), "42.00")
                .then().statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/transfer.json"));
    }

    @Test
    @DisplayName("Error responses (400 / 401 / 404 / 422)")
    void errors() {
        TestCustomer customer = BankClient.createTestCustomer(100_000, 0);
        BankClient client = BankClient.signedInAs(customer);
        client.transfer(customer.checking().id(), customer.savings().id(), "abc")
                .then().statusCode(400).body(matchesJsonSchemaInClasspath("schemas/error.json"));
        client.transfer(customer.checking().id(), customer.savings().id(), "5000.00")
                .then().statusCode(422).body(matchesJsonSchemaInClasspath("schemas/error.json"));
        client.transactions("acc_does_not_exist")
                .then().statusCode(404).body(matchesJsonSchemaInClasspath("schemas/error.json"));
        new BankClient().accounts()
                .then().statusCode(401).body(matchesJsonSchemaInClasspath("schemas/error.json"));
    }
}
