package com.ledgerline.ui.tests;

import com.ledgerline.ui.driver.DriverFactory;
import com.ledgerline.ui.pages.AccountsPage;
import com.ledgerline.ui.pages.LoginPage;
import com.ledgerline.ui.support.TestCustomers;
import com.ledgerline.ui.support.TestCustomers.Customer;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/** A fresh browser for every test method, closed afterwards even when the test fails. */
public abstract class BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void ledgerlineIsRunning() {
        TestCustomers.create("Preflight Check", 100, 0); // fails fast with a clear message if the server is down
    }

    @BeforeMethod(alwaysRun = true)
    public void startBrowser() {
        DriverFactory.start();
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        DriverFactory.quit();
    }

    protected WebDriver driver() {
        return DriverFactory.get();
    }

    /** A new customer, already signed in, on the accounts page. */
    protected AccountsPage signedIn(Customer customer) {
        return new LoginPage(driver()).open().signInAs(customer);
    }

    protected static Customer customer(long checkingCents, long savingsCents) {
        return TestCustomers.create("Taylor Morgan", checkingCents, savingsCents);
    }
}
