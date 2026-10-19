package com.ledgerline.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** A throwaway customer created for one test, so tests never share balances or lockout state. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestCustomer(String username, String password, String name, List<Account> accounts) {

    public Account checking() {
        return account("checking");
    }

    public Account savings() {
        return account("savings");
    }

    private Account account(String type) {
        return accounts.stream()
                .filter(a -> a.type().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + type + " account for " + username));
    }
}
