package com.ledgerline.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AccountsPage extends BasePage {

    private static final By WELCOME = By.id("welcome");
    private static final By ACCOUNT_LIST = By.cssSelector("#accounts .account");
    private static final By LIMIT = By.id("limit");
    private static final By TRANSFER = testId("btn-transfer");
    private static final By SIGN_OUT = testId("btn-logout");

    public AccountsPage(WebDriver driver) {
        super(driver);
    }

    public AccountsPage open() {
        open("#/accounts");
        return waitUntilLoaded();
    }

    public AccountsPage waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("view-accounts")));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(WELCOME, "Welcome back,"));
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(ACCOUNT_LIST, 1));
        return this;
    }

    public String welcome() {
        return visible(WELCOME).getText();
    }

    /** Displayed balance of "checking" or "savings", e.g. "$1,000.00". */
    public String balance(String type) {
        return visible(By.cssSelector("[data-testid='account-" + type + "'] .acct-balance")).getText();
    }

    public AccountsPage waitForBalance(String type, String expected) {
        wait.until(ExpectedConditions.textToBe(By.cssSelector("[data-testid='account-" + type + "'] .acct-balance"), expected));
        return this;
    }

    public String limitText() {
        return visible(LIMIT).getText();
    }

    public TransferPage goToTransfer() {
        visible(TRANSFER).click();
        return new TransferPage(driver).waitUntilLoaded();
    }

    public HistoryPage openHistory(String type) {
        visible(By.cssSelector("[data-testid='account-" + type + "']")).click();
        return new HistoryPage(driver).waitUntilLoaded();
    }

    public LoginPage signOut() {
        visible(SIGN_OUT).click();
        LoginPage login = new LoginPage(driver);
        wait.until(d -> login.isDisplayed());
        return login;
    }
}
