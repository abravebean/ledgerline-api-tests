package com.ledgerline.api.steps;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.support.Money;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferSteps {

    private final ScenarioContext ctx;

    public TransferSteps(ScenarioContext ctx) {
        this.ctx = ctx; // injected by cucumber-picocontainer
    }

    @Given("I am a customer with {money} in checking and {money} in savings")
    public void iAmACustomer(Long checking, Long savings) {
        ctx.me = BankClient.createTestCustomer(checking, savings);
        ctx.client = BankClient.signedInAs(ctx.me);
    }

    @Given("I have already transferred {money} from {account} to {account} today")
    public void alreadyTransferred(Long cents, String from, String to) {
        transfer(Money.plain(cents), from, to, null);
        assertThat(ctx.last().statusCode()).isEqualTo(201);
    }

    @When("I transfer {money} from {account} to {account}")
    public void iTransferMoney(Long cents, String from, String to) {
        transfer(Money.plain(cents), from, to, null);
    }

    @When("I transfer {string} from {account} to {account}")
    public void iTransferRaw(String amount, String from, String to) {
        transfer(amount, from, to, null);
    }

    @When("I transfer {money} from {account} to {account} with idempotency key {string}")
    public void iTransferWithKey(Long cents, String from, String to, String key) {
        transfer(Money.plain(cents), from, to, key);
    }

    @Then("the transfer is accepted")
    public void accepted() {
        assertThat(ctx.last().statusCode()).as(ctx.last().asString()).isEqualTo(201);
    }

    @Then("the transfer is refused with {string}")
    public void refused(String message) {
        assertThat(ctx.last().statusCode()).isEqualTo(422);
        assertThat(ctx.last().jsonPath().getString("error")).contains(message);
    }

    @Then("the response status is {int}")
    public void status(int expected) {
        assertThat(ctx.last().statusCode()).as(ctx.last().asString()).isEqualTo(expected);
    }

    @And("my {account} balance is {money}")
    public void myBalance(String type, Long cents) {
        assertThat(ctx.client.balanceCents(type)).isEqualTo(cents);
    }

    @And("{account} history shows a {string} entry of {money}")
    public void historyShows(String type, String description, Long cents) {
        List<Map<String, Object>> rows = ctx.client.transactions(ctx.accountId(type))
                .then().statusCode(200).extract().jsonPath().getList("transactions");
        assertThat(rows)
                .anySatisfy(row -> {
                    assertThat(row.get("description")).isEqualTo(description);
                    assertThat(((Number) row.get("amountCents")).longValue()).isEqualTo(cents);
                });
    }

    @Then("the last response is a replay of the first")
    public void replay() {
        Response first = ctx.responses.get(0);
        Response last = ctx.last();
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(last.statusCode()).isEqualTo(200);
        assertThat(last.getHeader("Idempotent-Replay")).isEqualTo("true");
        assertThat(last.jsonPath().getString("transfer.id")).isEqualTo(first.jsonPath().getString("transfer.id"));
    }

    private void transfer(String amount, String from, String to, String key) {
        ctx.responses.add(ctx.client.transfer(ctx.accountId(from), ctx.accountId(to), amount, key));
    }
}
