package com.jmmm.dispatch.service;

import com.jmmm.dispatch.message.DispatchCompleted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.message.OrderDispatched;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

import static java.util.UUID.randomUUID;

// Consumer
// 2. Servicio que solo procesa el evento consumido.
// Cada vez que se consuma un event order.created, ahora vamos a emitir un event order.dispatched
// y un segundo event dispatch.tracking
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";

    private static final UUID APPLICATION_ID = randomUUID();

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(String key, OrderCreated orderCreated) throws Exception {
        DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                .orderId(orderCreated.getOrderId())
                .build();
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

        OrderDispatched orderDispatched = OrderDispatched.builder()
                        .orderId(orderCreated.getOrderId())
                        .processedById(APPLICATION_ID)
                        .notes("Dispatched: " + orderCreated.getItem())
                        .build();
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispatched).get();

        DispatchCompleted dispatchCompleted = DispatchCompleted.builder()
                .orderId(orderCreated.getOrderId())
                .date(LocalDate.now().toString())
                .build();
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompleted).get();

        log.info("Sent messages: key: " + key + " - orderId: " + orderCreated.getOrderId() + " - processedById: " + APPLICATION_ID);
    }
}
