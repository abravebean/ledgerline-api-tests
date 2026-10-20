package com.ledgerline.ui.tests;

import com.ledgerline.ui.pages.AccountsPage;
import com.ledgerline.ui.pages.LoginPage;
import com.ledgerline.ui.support.TestCustomers.Customer;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SignInTest extends BaseTest {

    @Test(groups = {"smoke"}, description = "A customer signs in, sees their accounts, and signs out")
    public void signInAndOut() {
        Customer customer = customer(100_000, 500_000);

        AccountsPage accounts = signedIn(customer);
        assertThat(accounts.welcome()).isEqualTo("Welcome back, " + customer.firstName());
        assertThat(accounts.balance("checking")).isEqualTo("$1,000.00");
        assertThat(accounts.balance("savings")).isEqualTo("$5,000.00");

        LoginPage login = accounts.signOut();
        assertThat(login.isDisplayed()).isTrue();
    }

    @Test(groups = {"regression", "security"}, description = "A wrong password shows an error, clears the field and stays signed out")
    public void wrongPasswordIsRejected() {
        Customer customer = customer(100_000, 0);
        LoginPage login = new LoginPage(driver()).open().submit(customer.username(), "not-the-password");

        assertThat(login.errorAfterSubmit("Invalid username or password")).isEqualTo("Invalid username or password");
        assertThat(login.passwordField().getAttribute("value")).isEmpty();
        assertThat(login.isDisplayed()).isTrue();
    }

    @Test(groups = {"regression", "security"}, description = "Three wrong passwords lock sign-in, even for the right password")
    public void lockoutAfterThreeFailures() {
        Customer customer = customer(100_000, 0);
        LoginPage login = new LoginPage(driver()).open();

        login.submit(customer.username(), "wrong-1").errorAfterSubmit("Invalid username or password");
        login.submit(customer.username(), "wrong-2");
        login.submit(customer.username(), "wrong-3");
        assertThat(login.errorAfterSubmit("Try again in 15 minutes")).contains("Too many failed attempts");

        login.submit(customer.username(), customer.password());
        assertThat(login.errorAfterSubmit("Try again in 15 minutes")).contains("Too many failed attempts");
        assertThat(login.isDisplayed()).isTrue();
    }

    @Test(groups = {"regression", "security"}, description = "The password field masks what the customer types")
    public void passwordIsMasked() {
        LoginPage login = new LoginPage(driver()).open();
        assertThat(login.passwordField().getAttribute("type")).isEqualTo("password");
    }
}
