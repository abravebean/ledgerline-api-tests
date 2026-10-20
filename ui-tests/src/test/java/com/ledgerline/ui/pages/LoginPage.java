package com.ledgerline.ui.pages;

import com.ledgerline.ui.support.TestCustomers.Customer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {

    private static final By USERNAME = testId("input-username");
    private static final By PASSWORD = testId("input-password");
    private static final By SUBMIT = By.cssSelector("#login-form button[type='submit']");
    private static final By ERROR = testId("alert-error");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        open("");
        visible(USERNAME);
        return this;
    }

    public AccountsPage signInAs(Customer customer) {
        submit(customer.username(), customer.password());
        return new AccountsPage(driver).waitUntilLoaded();
    }

    /**
     * Submits the form and waits for the server's answer. The app clears the password field once
     * the response arrives (on success and failure), and disables the button while it waits,
     * so this never clicks a disabled button or races the previous attempt.
     */
    public LoginPage submit(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT)).click();
        wait.until(d -> d.findElements(PASSWORD).isEmpty() || d.findElement(PASSWORD).getAttribute("value").isEmpty());
        return this;
    }

    /** Waits for the error alert to show this text (it replaces the previous message each time). */
    public String errorAfterSubmit(String expectedFragment) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(ERROR, expectedFragment));
        return driver.findElement(ERROR).getText();
    }

    public boolean isDisplayed() {
        return isShown(USERNAME);
    }

    public WebElement passwordField() {
        return visible(PASSWORD);
    }
}
