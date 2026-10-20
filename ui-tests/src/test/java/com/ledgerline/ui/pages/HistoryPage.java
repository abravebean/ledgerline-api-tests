package com.ledgerline.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class HistoryPage extends BasePage {

    private static final By TITLE = By.id("history-title");
    private static final By ROWS = By.cssSelector("#history-rows tr");

    public HistoryPage(WebDriver driver) {
        super(driver);
    }

    /** Opens an account's history directly by ID, the way a tampered link would. */
    public HistoryPage openById(String accountId) {
        open("#/accounts/" + accountId);
        return waitUntilLoaded();
    }

    public HistoryPage waitUntilLoaded() {
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(TITLE, "Account")));
        return this;
    }

    public String title() {
        return visible(TITLE).getText();
    }

    /** One row per transaction, newest first: [date, description, amount, balance]. */
    public List<List<String>> rows() {
        return driver.findElements(ROWS).stream()
                .map(tr -> tr.findElements(By.tagName("td")).stream().map(WebElement::getText).toList())
                .toList();
    }
}
