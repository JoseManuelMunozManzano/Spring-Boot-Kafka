package com.jmmm.tracking.integration;

import com.jmmm.dispatch.message.DispatchCompleted;
import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.dispatch.message.TrackingStatusUpdated;
import com.jmmm.tracking.TrackingConfiguration;
import com.jmmm.tracking.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

@SpringBootTest(classes = {TrackingConfiguration.class})
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
@Slf4j
public class TrackingStatusIntegrationTest {

    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String TRACKING_STATUS_TOPIC = "tracking.status";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaTestListener testListener;

    // Para utilizar nuestro test consumer en nuestras pruebas, tenemos que añadirlo al
    // contexto de Spring.
    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }
    }

    // Nuestro test consumer.
    @KafkaListener(groupId = "KafkaIntegrationTests", topics = TRACKING_STATUS_TOPIC)
    public static class KafkaTestListener {
        AtomicInteger trackingStatusCounter = new AtomicInteger(0);

        @KafkaHandler
        void receiveTrackingStatus(@Payload TrackingStatusUpdated payload) {
            log.debug("Received TrackingStatus: " + payload);
            trackingStatusCounter.incrementAndGet();
        }
    }

    @BeforeEach
    void setup() {
        testListener.trackingStatusCounter.set(0);

        registry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    void testDispatchPreparingTrackingStatusFlow() throws Exception {
        DispatchPreparing dispatchPreparing = TestEventData.builDispatchPreparingEvent(randomUUID());
        sendMessage(DISPATCH_TRACKING_TOPIC, dispatchPreparing);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.trackingStatusCounter::get, equalTo(1));
    }

    @Test
    void testDispatchCompletedTrackingStatusFlow() throws Exception {
        DispatchCompleted dispatchCompleted = TestEventData.buildDispatchCompletedEvent(randomUUID(), LocalDate.now().toString());
        sendMessage(DISPATCH_TRACKING_TOPIC, dispatchCompleted);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.trackingStatusCounter::get, equalTo(1));
    }

    private void sendMessage(String topic, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder.
                withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
