package com.jmmm.dispatch.integration;

import com.jmmm.dispatch.DispatchConfiguration;
import com.jmmm.dispatch.message.DispatchCompleted;
import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.message.OrderDispatched;
import com.jmmm.dispatch.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.jmmm.dispatch.integration.WiremockUtils.stubWiremock;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
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
// Indicamos @AutoConfigureWireMock e indicamos un port con valor 0 para que asigne un puerto aleatorio no utilizado
// en tiempo de testing, en el que escuchará la instancia Wiremock.
// Importante actualizado application-test.properties con el valor de stockServiceEndpoint
@SpringBootTest(classes = {DispatchConfiguration.class})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true, partitions = 1)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class OrderDispatchIntegrationTest {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    // DEAD LETTER TOPIC
    private final static String ORDER_CREATED_DLT_TOPIC = "order.created-dlt";

    // Nos aseguramos que nuestro ListenerContainer Bean ha tenido las particiones
    // asignadas antes de que se ejecute la prueba.
    // Si la prueba se ejecuta antes de que esto suceda, entonces el consumer de la
    // aplicación no estará listo para consumir events, ya que no estará escuchando
    // a las particiones de los topic.
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    // Lo utilizamos para enviar events desde nuestros tests, es decir el producer.
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

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

    /**
     * Use this receiver to consume messages from the outbound topics.
     */
    // Nuestro test consumer.
    // Nos permite hacer aserciones sobre los events consumidos.
    // Para los tests, solo vamos a hacer seguimiento del número de events recibidos sobre cada topic.
    @KafkaListener(groupId = "KafkaIntegrationTest", topics = {DISPATCH_TRACKING_TOPIC, ORDER_DISPATCHED_TOPIC, ORDER_CREATED_DLT_TOPIC})
    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);
        AtomicInteger dispatchCompletedCounter = new AtomicInteger(0);
        AtomicInteger orderCreatedDLTCounter = new AtomicInteger(0);

        @KafkaHandler
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchPreparing payload) {
            log.debug("Received DispatchPreparing key: {} - payload: {}", key, payload);
            // Confirmamos que tenemos los valores
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderDispatched(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched key: {} - payload: {}", key, payload);
            // Confirmamos que tenemos los valores
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderDispatchedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchCompleted(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchCompleted payload) {
            log.debug("Received DispatchCompleted key: {} - payload: {}", key, payload);
            // Confirmamos que tenemos los valores
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchCompletedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderCreatedDLT(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderCreated payload) {
            log.debug("Received OrderCreated DLT key: {} - payload: {}", key, payload);
            // Confirmamos que tenemos los valores
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderCreatedDLTCounter.incrementAndGet();
        }
    }

    // Para asegurarnos de que hay suficiente tiempo para que se asignen los topic partitions
    // y el consumer está listo para consumir el evento enviado por el test.
    @BeforeEach
    void setUp() {
        testListener.dispatchPreparingCounter.set(0);
        testListener.orderDispatchedCounter.set(0);
        testListener.dispatchCompletedCounter.set(0);
        testListener.orderCreatedDLTCounter.set(0);

        WiremockUtils.reset();

        // Wait until the partitions are assigned. The application listener container has one topic and the test
        // listener container has multiple topics, so take that into account when awaiting for topic assignment.
        registry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container,
                        Objects.requireNonNull(container.getContainerProperties().getTopics()).length * embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    /**
     * Send in an order.created event and ensure the expected outbound events are emitted.  The call to the stock service
     * is stubbed to return a 200 Success.
     */
    @Test
    void testOrderDispatchFlow_Success() throws Exception {
        stubWiremock("/api/stock?item=my-item", 200, "true");

        OrderCreated orderCreated = TestEventData.builOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
        assertThat(testListener.orderCreatedDLTCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to return a 400 Bad Request which results in a not-retryable exception
     * being thrown, so the outbound events are never sent.
     */
    @Test
    void testOrderDispatchFlow_NotRetryableException() throws Exception {
        stubWiremock("/api/stock?item=my-item", 400, "Bad Request");

        OrderCreated orderCreated = TestEventData.builOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLTCounter::get, equalTo(1));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.orderDispatchedCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to initially return a 503 Service Unavailable response, resulting in a
     * retryable exception being thrown. On the subsequent attempt it is stubbed to then succeed, so the outbound events
     * are sent.
     */
    @Test
    void testOrderDispatchFlow_RetryThenSuccess() throws Exception {
        stubWiremock("/api/stock?item=my-item", 503, "Service unavailable", "failOnce", STARTED, "succeedNextTime");
        stubWiremock("/api/stock?item=my-item", 200, "true", "failOnce", "succeedNextTime", "succeedNextTime");

        OrderCreated orderCreated = TestEventData.builOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
        assertThat(testListener.orderCreatedDLTCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to initially return a 503 Service Unavailable response.  This results in
     * retryable exceptions being thrown continually, eventually exceeding the retry limit.  The event is sent to the
     * dead letter topic, and the outbound events are never sent.
     */
    @Test
    public void testOrderDispatchFlow_RetryUntilFailure() throws Exception {
        stubWiremock("/api/stock?item=my-item", 503, "Service unavailable");

        OrderCreated orderCreated = TestEventData.builOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(5, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLTCounter::get, equalTo(1));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.orderDispatchedCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    // Indicamos Object para poder reutilizarlo en otros tests.
    private void sendMessage(String topic, String key, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
