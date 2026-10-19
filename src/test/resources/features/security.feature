@api @security
Feature: Keeping customers' money private
  Customers must only ever see and move their own money,
  and the sign-in must resist guessing.

  Scenario: I can't see another customer's account
    Given another customer "Alice" has $1,000.00 in checking
    And I am a customer with $10.00 in checking and $0.00 in savings
    When I ask for Alice's checking history
    Then the response status is 404

  Scenario: I can't move another customer's money
    Given another customer "Alice" has $1,000.00 in checking
    And I am a customer with $10.00 in checking and $0.00 in savings
    When I try to transfer $500.00 from Alice's checking to my savings
    Then the response status is 404
    And Alice's checking balance is still $1,000.00

  Scenario: Three wrong passwords lock the sign-in
    Given I am a customer with $10.00 in checking and $0.00 in savings
    When I sign in with a wrong password 3 times
    Then signing in with my real password is refused with status 423

  Scenario: A signed-out session can't be reused
    Given I am a customer with $10.00 in checking and $0.00 in savings
    When I sign out
    Then my old session token is rejected with status 401
