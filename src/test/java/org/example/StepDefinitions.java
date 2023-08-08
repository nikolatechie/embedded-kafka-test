package org.example;

import io.cucumber.java.en.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;

@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
public class StepDefinitions {
    private final EmbeddedKafkaBroker embeddedKafka = new EmbeddedKafkaBroker(1, true, "test-topic");
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(StepDefinitions.class);

    @Given("I have an Embedded Kafka instance")
    public void iHaveAnEmbeddedKafkaInstance() {
        embeddedKafka.kafkaPorts(9092);
        embeddedKafka.afterPropertiesSet();

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
        LOGGER.info("Embedded Kafka is running");
    }

    @When("I produce a message with topic {string} and content {string}")
    public void iProduceAMessageWithTopicAndContent(String topic, String message) {
        kafkaTemplate.send(topic, message);
        kafkaTemplate.flush();
        LOGGER.info("[{}] Sending a message: {}", topic, message);
    }

    @Then("I should consume the message with topic {string} and content {string}")
    public void iShouldConsumeTheMessageWithTopicAndContent(String topic, String expectedMessage) {
        // Create consumer properties
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Create a Kafka consumer
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> consumer = consumerFactory.createConsumer();

        // Subscribe to the topic
        consumer.subscribe(Collections.singleton(topic));

        // Set up a CountDownLatch to wait for records
        CountDownLatch latch = new CountDownLatch(1);

        // Consume records
        new Thread(() -> {
            try {
                ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, topic);
                LOGGER.info("[{}] Consuming a message: {}", record.topic(), record.value());
                assertEquals(expectedMessage, record.value());
                latch.countDown();
            } finally {
                consumer.close();
            }
        }).start();

        // Wait for the latch to release (timeout: 10 seconds)
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Handle exception
        }
    }
}