package com.ledgerline.api.steps;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class SecuritySteps {

    private final ScenarioContext ctx;
    private String oldToken;

    public SecuritySteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Given("another customer {string} has {money} in checking")
    public void anotherCustomer(String name, Long cents) {
        ctx.others.put(name, BankClient.createTestCustomer(cents, 0));
    }

    @When("I ask for {word}'s checking history")
    public void askForOthersHistory(String name) {
        ctx.responses.add(ctx.client.transactions(ctx.others.get(name).checking().id()));
    }

    @When("I try to transfer {money} from {word}'s checking to my savings")
    public void transferFromOthers(Long cents, String name) {
        String amount = com.ledgerline.api.support.Money.plain(cents);
        ctx.responses.add(ctx.client.transfer(ctx.others.get(name).checking().id(), ctx.me.savings().id(), amount));
    }

    @And("{word}'s checking balance is still {money}")
    public void othersBalance(String name, Long cents) {
        TestCustomer other = ctx.others.get(name);
        assertThat(BankClient.signedInAs(other).balanceCents("checking")).isEqualTo(cents);
    }

    @When("I sign in with a wrong password {int} times")
    public void wrongPassword(int times) {
        BankClient fresh = new BankClient();
        for (int i = 0; i < times; i++) {
            fresh.login(ctx.me.username(), "wrong-password");
        }
    }

    @Then("signing in with my real password is refused with status {int}")
    public void realPasswordRefused(int status) {
        assertThat(new BankClient().login(ctx.me.username(), ctx.me.password()).statusCode()).isEqualTo(status);
    }

    @When("I sign out")
    public void signOut() {
        oldToken = ctx.client.sessionToken();
        ctx.client.logout().then().statusCode(204);
    }

    @Then("my old session token is rejected with status {int}")
    public void oldTokenRejected(int status) {
        assertThat(BankClient.withSessionToken(oldToken).get("/accounts").statusCode()).isEqualTo(status);
    }
}
