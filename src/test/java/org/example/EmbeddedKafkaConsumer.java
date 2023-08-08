package org.example;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class EmbeddedKafkaConsumer implements Runnable {
    private final EmbeddedKafkaBroker embeddedKafka;
    private final String topic;
    private String receivedMessage;
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedKafkaConsumer.class);

    public EmbeddedKafkaConsumer(EmbeddedKafkaBroker embeddedKafka, String topic) {
        this.embeddedKafka = embeddedKafka;
        this.topic = topic;
    }

    @Override
    public void run() {
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
        try {
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, topic);
            LOGGER.info("[{}] Consuming a message: {}", record.topic(), record.value());
            receivedMessage = record.value();
            latch.countDown();
        } finally {
            consumer.close();
        }

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }
}
