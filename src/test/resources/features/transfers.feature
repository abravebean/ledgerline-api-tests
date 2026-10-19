@api @transfers
Feature: Moving money between my own accounts
  As a Ledgerline customer
  I want to move money between my checking and savings accounts
  So that it is where I need it, without ever being overdrawn

  Background:
    Given I am a customer with $1,000.00 in checking and $5,000.00 in savings

  Scenario: A transfer moves money and records it on both accounts
    When I transfer $250.00 from checking to savings
    Then the transfer is accepted
    And my checking balance is $750.00
    And my savings balance is $5,250.00
    And checking history shows a "Transfer to High-Yield Savings" entry of -$250.00

  Scenario Outline: The amount is checked at its boundaries
    When I transfer "<amount>" from checking to savings
    Then the response status is <status>
    And my checking balance is <checking after>

    Examples: Valid amounts
      | amount  | status | checking after |
      | 0.01    | 201    | $999.99        |
      | 1000.00 | 201    | $0.00          |

    Examples: Invalid amounts
      | amount  | status | checking after |
      | 0       | 400    | $1,000.00      |
      | -1      | 400    | $1,000.00      |
      | 1.999   | 400    | $1,000.00      |
      | 1000.01 | 422    | $1,000.00      |

  Scenario: The daily limit counts every transfer made today
    Given I have already transferred $2,000.00 from savings to checking today
    When I transfer $500.01 from savings to checking
    Then the transfer is refused with "You can transfer $500.00 more today"
    When I transfer $500.00 from savings to checking
    Then the transfer is accepted

  Scenario: Pressing "Transfer" twice only moves the money once
    When I transfer $100.00 from checking to savings with idempotency key "double-click-001"
    And I transfer $100.00 from checking to savings with idempotency key "double-click-001"
    Then the last response is a replay of the first
    And my checking balance is $900.00
