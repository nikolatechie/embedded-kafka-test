Feature: Embedded Kafka Test

  Scenario Outline: Producing and consuming a message using Embedded Kafka
    Given I have an Embedded Kafka instance
    When I produce a message with topic "<TOPIC>" and content <MESSAGE>
    Then I should consume the message with topic "<TOPIC>" and content <MESSAGE>

    Examples: Topics and messages
      | TOPIC      | MESSAGE         |
      | test-topic | "Hello, Kafka!" |