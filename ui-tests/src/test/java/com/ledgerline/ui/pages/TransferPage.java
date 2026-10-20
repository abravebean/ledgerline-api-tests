package com.ledgerline.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class TransferPage extends BasePage {

    private static final By FROM = testId("transfer-from");
    private static final By TO = testId("transfer-to");
    private static final By AMOUNT = testId("transfer-amount");
    private static final By SUBMIT = By.id("transfer-submit");
    private static final By ERROR = testId("transfer-error");
    private static final By RECEIPT = testId("transfer-success");
    private static final By RECEIPT_AMOUNT = testId("receipt-amount");
    private static final By RECEIPT_REFERENCE = testId("receipt-reference");

    public TransferPage(WebDriver driver) {
        super(driver);
    }

    /** The view is shown only after the form has been reset and refilled, so typing can't be wiped. */
    public TransferPage waitUntilLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("transfer-form")));
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("[data-testid='transfer-from'] option"), 1));
        return this;
    }

    public TransferPage fill(String fromAccountId, String toAccountId, String amount) {
        new Select(visible(FROM)).selectByValue(fromAccountId);
        new Select(visible(TO)).selectByValue(toAccountId);
        type(AMOUNT, amount);
        return this;
    }

    public TransferPage submit() {
        visible(SUBMIT).click();
        return this;
    }

    /** A real double-click on the button, the way an impatient customer would. */
    public TransferPage doubleClickSubmit() {
        new Actions(driver).doubleClick(visible(SUBMIT)).perform();
        return this;
    }

    public String receiptAmount() {
        visible(RECEIPT);
        return visible(RECEIPT_AMOUNT).getText();
    }

    public String receiptReference() {
        return visible(RECEIPT_REFERENCE).getText();
    }

    public String error() {
        return visible(ERROR).getText();
    }

    public boolean amountFlaggedInvalid() {
        return "true".equals(driver.findElement(AMOUNT).getAttribute("aria-invalid"));
    }

    public boolean receiptShown() {
        return isShown(RECEIPT);
    }

    public AccountsPage backToAccounts() {
        WebElement link = visible(By.cssSelector("[data-testid='transfer-success'] a[href='#/accounts']"));
        link.click();
        return new AccountsPage(driver).waitUntilLoaded();
    }
}
