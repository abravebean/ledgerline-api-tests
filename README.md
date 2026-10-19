# Ledgerline API Tests (Java · REST Assured · Cucumber)

[![API tests](https://github.com/abravebean/ledgerline-api-tests/actions/workflows/api-tests.yml/badge.svg)](https://github.com/abravebean/ledgerline-api-tests/actions/workflows/api-tests.yml)

API test automation in **Java 17** for **Ledgerline**, the fictional demo bank in my [Bank Automation Test Bench](https://github.com/abravebean/Bank-automation-test-bench) portfolio project. It covers the same banking risks as the TypeScript suite over there, written in the stack most bank QA teams use: **Maven, JUnit 5, REST Assured, Cucumber (BDD) and AssertJ**.

Every push, pull request and nightly run in GitHub Actions starts a fresh Ledgerline server and runs the whole suite against it.

> Ledgerline is a demo bank I built for testing. Nothing here touches a real bank.

## What it tests

| Area | Examples | Where |
|---|---|---|
| **Boundary values** | $0.00, $0.01, one cent under, at and over the balance, 3 decimal places, `1e2`, empty | `TransferAmountBoundaryTest` (`@ParameterizedTest` table), `transfers.feature` (Scenario Outline) |
| **Business rules** | $2,500.00 daily limit, same-account transfers refused, history running balances add up | `BusinessRulesTest`, `transfers.feature` |
| **Authorization (OWASP A01)** | A customer can't read or move another customer's money (IDOR returns 404, not 403); every route needs a session | `AuthorizationTest`, `security.feature` |
| **Sign-in security (OWASP A07)** | Lockout after 3 failures, same response for real and fake usernames, `HttpOnly` + `SameSite=Strict` cookie, sign-out kills the session server-side | `SignInSecurityTest`, `security.feature` |
| **Double spending (CWE-362)** | `Idempotency-Key` replay moves money once; 20 simultaneous $100 transfers from $1,000: exactly 10 succeed and the balance never goes negative | `IdempotencyAndConcurrencyTest` |
| **API contracts** | Responses validated against JSON Schemas (masked account numbers, integer cents, money format) | `ContractTest`, `src/test/resources/schemas/` |

## How it's built

```
src/test/java/com/ledgerline/api/
├── client/BankClient.java        REST Assured client: one instance = one signed-in customer
├── config/Config.java            Base URL from LEDGERLINE_BASE_URL, -Dledgerline.baseUrl, or localhost
├── model/                        Account, TestCustomer (Java records, mapped by Jackson)
├── support/                      Money (cents <-> "$1,250.00", no floating point), LedgerlineAvailable
├── tests/                        JUnit 5 test classes
├── steps/                        Cucumber step definitions, parameter types, hooks
└── runner/RunCucumberTest.java   Runs the .feature files on the JUnit Platform
src/test/resources/
├── features/                     transfers.feature, security.feature (Gherkin)
└── schemas/                      JSON Schemas for the contract tests
```

Design choices worth knowing:

- **Every test creates its own customer** through a test-only route (`POST /bank/api/test/customers`, only on when the server starts with `DEMO_BANK_TEST_HOOKS=true`). Tests never share balances, daily-limit counters or lockout state, so the order they run in doesn't matter and none of them depend on seed data.
- **The client holds the session token itself** instead of a shared cookie store. Two `BankClient`s are two independent customers, and one client is safe to use from 20 threads at once in the concurrency test.
- **Money is integer cents.** Amounts are compared as `long`, and the `Money` helper uses `BigDecimal`, never `double`.
- **Cucumber steps share state through PicoContainer** (`ScenarioContext`), a new instance per scenario. Custom `{money}` and `{account}` parameter types keep the Gherkin readable: `When I transfer $250.00 from checking to savings`.
- **Fail fast with a clear message.** Before any test runs, `LedgerlineAvailable` checks the server is up with its test hooks on, and explains how to start it if not.

## Run it locally

You need Java 17+, Maven, and Node.js 20+ (for Ledgerline).

```bash
# 1. Start Ledgerline with its test hooks on (in the Bank-automation-test-bench folder)
DEMO_BANK_TEST_HOOKS=true npm start

# 2. Run everything (in this folder)
mvn verify

# Only some of it
mvn test -Dtest=AuthorizationTest
mvn test -Dgroups=security            # JUnit tags: unit, rules, security, integrity, contract
mvn test -Dtest=RunCucumberTest       # just the Cucumber scenarios

# Against another host
LEDGERLINE_BASE_URL=http://localhost:4000 mvn verify
```

Reports land in `target/`: `cucumber-report.html` (open it in a browser), `cucumber.json`, and JUnit XML in `surefire-reports/`.

## CI

`.github/workflows/api-tests.yml` checks out this repo and Ledgerline, starts Ledgerline, and runs `mvn verify` on Temurin 17. Failures show up as annotations on the run and the PR (file, line and assertion message), a results table goes in the job summary, and the Cucumber HTML report is uploaded as an artifact. A nightly run catches a Ledgerline change that breaks the API contract even when this repo hasn't changed.

## Talking about it in an interview

**The 30-second version:** "My main portfolio project is a demo bank with a TypeScript and Playwright test suite. Bank QA teams mostly work in Java, so I wrote a second suite for the same API in Java with REST Assured, JUnit 5 and Cucumber. It covers boundary values, the daily limit, IDOR, lockout, idempotency and a 20-thread race condition test, plus JSON Schema contract tests. It runs on every push and nightly in GitHub Actions against a fresh copy of the bank."

Questions you might get:

- **Why REST Assured?** It's the standard Java library for API tests: given/when/then syntax, built-in JSON path and schema validation, and it plugs into JUnit. I wrapped it in a small `BankClient` so the tests read like the business rule, not like HTTP plumbing.
- **Why both JUnit and Cucumber?** Cucumber scenarios are the readable acceptance criteria a product owner can review. The edge cases (big boundary tables, concurrency, cookie flags) are clearer and faster as plain JUnit tests. Use each where it's strongest.
- **How do you keep tests independent?** Each test creates its own customer through a test-only API, so there's no shared data and no cleanup to forget. That's also what makes parallel runs safe.
- **How do you test a race condition?** Twenty threads wait on a `CountDownLatch`, all fire a $100 transfer from a $1,000 account at the same moment, and the test asserts exactly 10 got 201, 10 got 422, and the balance ended at exactly $0.00.
- **Why does the IDOR test expect 404 and not 403?** A 403 confirms the account exists. Returning 404 for someone else's account gives an attacker nothing to enumerate.
- **Why integer cents?** `0.1 + 0.2` isn't `0.3` in floating point. Banks store minor units, so the tests compare cents as `long` and convert with `BigDecimal`.
