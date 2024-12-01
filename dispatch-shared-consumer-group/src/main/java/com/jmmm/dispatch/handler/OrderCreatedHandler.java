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
    // Para el paso 5 indicado en las notas del README añadimos el containerFactory.
    // Este último no hacía falta cuando lo teníamos en application.properties.
    @KafkaListener(id = "orderConsumerClient",
                   topics = "order.created",
                   groupId = "dispatch.order.created.consumer",
                   containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(OrderCreated payload) {
        log.info("Received message: payload: " + payload);

        // Manejamos una posible excepción que pudiera venir de DispatchService.
        // Si no la manejamos aquí, el event de order.created sería reintentado una y otra vez.
        //
        // Registramos el error con un log y se marca como completado el event de creación de orden.
        // Así no será consumido de nuevo.
        // Más adelante revisitaremos esto para tratar lo que se llama dead letter topics.
        try {
            dispatchService.process(payload);
        } catch (Exception e) {
            log.error("Processing failure", e);
        }
    }
}
