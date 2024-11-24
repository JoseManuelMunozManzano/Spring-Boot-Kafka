package com.jmmm.dispatch.handler;

import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Consumer
// 1. Este manejador solo es responsable de consumir el evento.
//
// Una vez el evento se ha consumido llamamos a un servicio que pueda procesar el evento.
// Ver service/DispatchService.java
//
// Esta separación de funciones ayuda con las pruebas unitarias.
@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreatedHandler {

    private final DispatchService dispatchService;

    // Con esta anotación Spring hace el trabajo pesado de consumir el evento
    // desde Kafka por nosotros y pasarlo a este listener.
    // Al listener le indicamos un id, los topics que debe escuchar y un id de grupo
    // que indica el consumer group al que debe pertenecer este consumer.
    @KafkaListener(id = "orderConsumerClient", topics = "order.created", groupId = "dispatch.order.created.consumer")
    public void listen(OrderCreated payload) {
        log.info("Received message: payload: " + payload);
        dispatchService.process(payload);
    }
}
