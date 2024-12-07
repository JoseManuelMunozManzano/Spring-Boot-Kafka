package com.jmmm.dispatch.handler;

import com.jmmm.dispatch.exception.NotRetryableException;
import com.jmmm.dispatch.exception.RetryableException;
import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// Consumer
// 1. Este manejador solo es responsable de consumir el evento.
//
// Una vez el evento se ha consumido llamamos a un servicio que pueda procesar el evento.
// Ver service/DispatchService.java
@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreatedHandler {

    private final DispatchService dispatchService;

    @KafkaListener(id = "orderConsumerClient",
                   topics = "order.created",
                   groupId = "dispatch.order.created.consumer",
                   containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition, @Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderCreated payload) {
        log.info("Received message: partition: {} - key: {} - orderId: {} - item: {}", partition, key, payload.getOrderId(), payload.getItem());

        try {
            dispatchService.process(key, payload);
        } catch (RetryableException e) {
            log.warn("Retryable exception: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("NotRetryable exception: {}", e.getMessage());
            throw new NotRetryableException(e);
        }
    }
}
