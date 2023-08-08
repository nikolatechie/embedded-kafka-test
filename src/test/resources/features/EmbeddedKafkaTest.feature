Feature: Embedded Kafka Test

  Scenario: Producing and consuming a message using Embedded Kafka
    Given I have an Embedded Kafka instance
    When I produce a message with topic "test-topic" and content "Hello, Kafka!"
    Then I should consume the message with topic "test-topic" and content "Hello, Kafka!"