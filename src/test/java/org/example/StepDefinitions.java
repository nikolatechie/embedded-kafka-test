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
    private static final Logger LOGGER = LoggerFactory.getLogger(StepDefinitions.class);

    @Given("I have an Embedded Kafka instance")
    public void iHaveAnEmbeddedKafkaInstance() {
        embeddedKafka.kafkaPorts(9092);
        embeddedKafka.afterPropertiesSet();
        LOGGER.info("Embedded Kafka is running");
    }

    @When("I produce a message with topic {string} and content {string}")
    public void iProduceAMessageWithTopicAndContent(String topic, String message) {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.send(topic, message);
        kafkaTemplate.flush();
        LOGGER.info("[{}] Sending a message: {}", topic, message);
    }

    @Then("I should consume the message with topic {string} and content {string}")
    public void iShouldConsumeTheMessageWithTopicAndContent(String topic, String expectedMessage) throws InterruptedException {
        EmbeddedKafkaConsumer consumer = new EmbeddedKafkaConsumer(embeddedKafka, topic);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        consumerThread.join();
        assertEquals(expectedMessage, consumer.getReceivedMessage());
    }
}