package com.jmmm.dispatch.integration;

import com.jmmm.dispatch.DispatchConfiguration;
import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.message.OrderDispatched;
import com.jmmm.dispatch.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

// Indicamos en @SpringBootTest las clases donde se declaran los beans de Spring (clases @Configuration)
// Indicamos @ActiveProfiles con un perfil `test` que nos permita sobreescribir las propiedades de test en
// un nuevo archivo de application.properties.
// Indicamos @EmbeddedKafka para usar el broker Kafka incrustado en Spring Kafka. Con controllerShutdown a true
// la prueba debe terminar limpiamente.
// https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki#error-running-integration-tests-with-spring-boot-32
// Ver la web de arriba para saber por qué se indica el número de particiones a partir de Spring Boot 3.2.0
// AUNQUE NO HARIA FALTA EN ESTE CASO, LO DEJO POR SI ACASO.
// Indicamos @DirtiesContext con classMode afterClass. Esto indica que el contexto de Spring no se carga
// entre cada méto-do de prueba, lo que puede aumentar enormemente los tiempos de prueba a medida que se
// añaden más y más pruebas.
// Indicamos @Slf4j para facilitar la inclusión de logs.
@SpringBootTest(classes = {DispatchConfiguration.class})
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true, partitions = 1)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class OrderDispatchIntegrationTest {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";

    // Nos aseguramos que nuestro ListenerContainer Bean ha tenido las particiones
    // asignadas antes de que se ejecute la prueba.
    // Si la prueba se ejecuta antes de que esto suceda, entonces el consumer de la
    // aplicación no estará listo para consumir events, ya que no estará escuchando
    // a las particiones de los topic.
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    // Lo utilizamos para enviar events desde nuestros tests.
    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private KafkaTestListener testListener;

    // Para utilizar nuestro test consumer en nuestras pruebas, tenemos que añadirlo al
    // contexto de Spring. Esto lo hacemos declarándolo como un Bean de Spring y añadiéndolo
    // a una configuración de pruebas.
    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }

    }

    // Nuestro test consumer.
    // Nos permite hacer asersiones sobre los events consumidos.
    // Para los tests, solo vamos a hacer seguimiento del número de events recibidos sobre cada topic.
    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = DISPATCH_TRACKING_TOPIC)
        void receiveDispatchPreparing(@Payload DispatchPreparing payload) {
            log.debug("Received DispatchPreparing: " + payload);
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = ORDER_DISPATCHED_TOPIC)
        void receiveDispatchPreparing(@Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched: " + payload);
            orderDispatchedCounter.incrementAndGet();
        }
    }

    @BeforeEach
    void setUp() {
        testListener.dispatchPreparingCounter.set(0);
        testListener.orderDispatchedCounter.set(0);

        registry.getListenerContainers().stream().forEach(container ->
                        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    void testOrderDispatchFlow() throws Exception {
        OrderCreated orderCreated = TestEventData.builOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, orderCreated);

        // Usamos awaitility para afirmar que cada contador se ha incrementado como se esperaba.
        // Esperamos como mucho 3sg, comprobando cada 100ms hasta que dispatchPreparingCounter
        // se incrementa a 1.
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
    }

    // Indicamos Object para poder reutilizarlo en otros tests.
    private void sendMessage(String topic, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
