package com.ledgerline.api.steps;

import com.ledgerline.api.client.BankClient;
import com.ledgerline.api.model.TestCustomer;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State shared by the step classes in one scenario. PicoContainer creates a new
 * instance for every scenario, so nothing leaks between scenarios.
 */
public class ScenarioContext {
    public TestCustomer me;
    public BankClient client;
    public final Map<String, TestCustomer> others = new HashMap<>();
    public final List<Response> responses = new ArrayList<>();

    public Response last() {
        return responses.get(responses.size() - 1);
    }

    public String accountId(String type) {
        return "checking".equals(type) ? me.checking().id() : me.savings().id();
    }
}
