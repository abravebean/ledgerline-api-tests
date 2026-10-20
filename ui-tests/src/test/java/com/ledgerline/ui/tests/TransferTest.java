package com.ledgerline.ui.tests;

import com.ledgerline.ui.pages.AccountsPage;
import com.ledgerline.ui.pages.HistoryPage;
import com.ledgerline.ui.pages.TransferPage;
import com.ledgerline.ui.support.TestCustomers.Customer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferTest extends BaseTest {

    @Test(groups = {"smoke"}, description = "A transfer shows a receipt, updates both balances and appears in the history")
    public void transferUpdatesBalancesAndHistory() {
        Customer customer = customer(100_000, 500_000);
        TransferPage transfer = signedIn(customer).goToTransfer()
                .fill(customer.checking().id(), customer.savings().id(), "250.00")
                .submit();

        assertThat(transfer.receiptAmount()).isEqualTo("$250.00");
        assertThat(transfer.receiptReference()).matches("LL-\\d+");

        AccountsPage accounts = transfer.backToAccounts();
        accounts.waitForBalance("checking", "$750.00");
        assertThat(accounts.balance("savings")).isEqualTo("$5,250.00");
        assertThat(accounts.limitText()).contains("Transferred today: $250.00");

        HistoryPage history = accounts.openHistory("checking");
        List<String> newest = history.rows().get(0);
        assertThat(newest.get(1)).isEqualTo("Transfer to High-Yield Savings");
        assertThat(newest.get(2)).isEqualTo("-$250.00");
        assertThat(newest.get(3)).isEqualTo("$750.00");
    }

    /** Boundary values and invalid input for a customer holding exactly $1,000.00 in checking. */
    @DataProvider(name = "rejectedAmounts", parallel = true)
    public Object[][] rejectedAmounts() {
        return new Object[][] {
                {"0", "Amount must be more than $0.00"},
                {"-20", "Enter an amount like 25 or 25.50"},
                {"12.345", "Enter an amount like 25 or 25.50"},
                {"abc", "Enter an amount like 25 or 25.50"},
                {"1e3", "Enter an amount like 25 or 25.50"},
                {"1000.01", "Insufficient funds"},
        };
    }

    @Test(dataProvider = "rejectedAmounts", groups = {"regression"}, description = "Invalid amounts are refused with a clear message and no money moves")
    public void invalidAmountIsRefused(String amount, String message) {
        Customer customer = customer(100_000, 0);
        TransferPage transfer = signedIn(customer).goToTransfer()
                .fill(customer.checking().id(), customer.savings().id(), amount)
                .submit();

        assertThat(transfer.error()).contains(message);
        assertThat(transfer.amountFlaggedInvalid()).as("amount field has aria-invalid").isTrue();
        assertThat(transfer.receiptShown()).isFalse();
        assertThat(new AccountsPage(driver()).open().balance("checking")).isEqualTo("$1,000.00");
    }

    @DataProvider(name = "acceptedBoundaries", parallel = true)
    public Object[][] acceptedBoundaries() {
        return new Object[][] {
                {"0.01", "$999.99"},   // smallest amount
                {"999.99", "$0.01"},   // one cent under the balance
                {"1000.00", "$0.00"},  // exactly the balance
        };
    }

    @Test(dataProvider = "acceptedBoundaries", groups = {"regression"}, description = "Amounts at the edges of what's allowed go through")
    public void boundaryAmountIsAccepted(String amount, String checkingAfter) {
        Customer customer = customer(100_000, 0);
        TransferPage transfer = signedIn(customer).goToTransfer()
                .fill(customer.checking().id(), customer.savings().id(), amount)
                .submit();

        transfer.receiptAmount();
        transfer.backToAccounts().waitForBalance("checking", checkingAfter);
    }

    @Test(groups = {"regression"}, description = "Double-clicking Transfer moves the money once")
    public void doubleClickMovesMoneyOnce() {
        Customer customer = customer(100_000, 0);
        TransferPage transfer = signedIn(customer).goToTransfer()
                .fill(customer.checking().id(), customer.savings().id(), "10.00")
                .doubleClickSubmit();

        assertThat(transfer.receiptAmount()).isEqualTo("$10.00");
        AccountsPage accounts = new AccountsPage(driver()).open().waitForBalance("checking", "$990.00");
        assertThat(accounts.balance("savings")).isEqualTo("$10.00");
    }

    @Test(groups = {"regression"}, description = "The $2,500.00 daily limit is enforced in the UI with the amount left")
    public void dailyLimitIsExplained() {
        Customer customer = customer(0, 1_000_000);
        TransferPage transfer = signedIn(customer).goToTransfer()
                .fill(customer.savings().id(), customer.checking().id(), "2000.00")
                .submit();
        transfer.receiptAmount();

        TransferPage second = transfer.backToAccounts().goToTransfer()
                .fill(customer.savings().id(), customer.checking().id(), "500.01")
                .submit();
        assertThat(second.error()).contains("You can transfer $500.00 more today");
    }
}
