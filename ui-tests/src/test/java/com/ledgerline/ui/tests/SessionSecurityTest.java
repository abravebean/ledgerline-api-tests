package com.ledgerline.ui.tests;

import com.ledgerline.ui.pages.AccountsPage;
import com.ledgerline.ui.pages.HistoryPage;
import com.ledgerline.ui.pages.LoginPage;
import com.ledgerline.ui.support.TestCustomers.Customer;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionSecurityTest extends BaseTest {

    @Test(groups = {"regression", "security"}, description = "Opening another customer's account by URL shows 'Account not found' (IDOR)")
    public void cannotOpenSomeoneElsesAccount() {
        Customer alice = customer(123_456, 0);
        Customer mallory = customer(100, 0);
        signedIn(mallory);

        HistoryPage history = new HistoryPage(driver()).openById(alice.checking().id());

        assertThat(history.title()).isEqualTo("Account not found");
        assertThat(history.rows()).isEmpty();
        assertThat(driver().getPageSource()).doesNotContain("$1,234.56");
    }

    @Test(groups = {"regression", "security"}, description = "After signing out, Back doesn't bring account data back")
    public void backButtonAfterSignOutShowsNoData() {
        Customer customer = customer(424_242, 0);
        AccountsPage accounts = signedIn(customer);
        assertThat(accounts.balance("checking")).isEqualTo("$4,242.42");

        accounts.signOut();
        driver().navigate().back();
        driver().navigate().refresh();

        LoginPage login = new LoginPage(driver());
        assertThat(login.isDisplayed()).isTrue();
        assertThat(driver().findElement(By.id("view-app")).isDisplayed()).isFalse();
    }

    @Test(groups = {"regression", "security"}, description = "The session cookie is HttpOnly and SameSite=Strict, so page scripts can't read it")
    public void sessionCookieIsProtected() {
        signedIn(customer(100_000, 0));

        Cookie session = driver().manage().getCookieNamed("ll_session");
        assertThat(session).as("session cookie is set").isNotNull();
        assertThat(session.isHttpOnly()).isTrue();
        assertThat(session.getSameSite()).isEqualTo("Strict");
        assertThat(session.getPath()).isEqualTo("/bank");

        Object visibleToScripts = ((JavascriptExecutor) driver()).executeScript("return document.cookie;");
        assertThat(String.valueOf(visibleToScripts)).doesNotContain("ll_session");
    }
}
