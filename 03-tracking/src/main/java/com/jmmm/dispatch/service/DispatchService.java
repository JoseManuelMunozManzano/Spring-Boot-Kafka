package com.jmmm.dispatch.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.message.OrderDispatched;

import lombok.RequiredArgsConstructor;

// Consumer
// 2. Servicio que solo procesa el evento consumido.
// Cada vez que se consuma un event order.created, ahora vamos a emitir un nuevo event order.dispatched
@Service
@RequiredArgsConstructor
public class DispatchService {

    // El nombre del topic suele venir de application.properties.
    // Lo indicamos como constante porque no hace falta complicarlo.
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";

    // KafkaTemplate proporciona métodos de utilidad para enviar y recibir events, con el fin
    // de enviar nuestro event saliente a Kafka.
    // Declarándolo como private y final, Lombok creará un constructor en el service que toma
    // el template como argumento y Spring instanciará el servicio con el KafkaTemplate al inicio
    // de la aplicación.
    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(OrderCreated orderCreated) throws Exception {
        // El payload
        OrderDispatched orderDispatched = OrderDispatched.builder()
                        .orderid(orderCreated.getOrderId())
                        .build();

        // Para que no sea asíncrono (por defecto), lo indicamos usando el método get()
        // sobre el objeto Future que se devuelve.
        // Por tanto, es necesario un acuse de recibo del envío por parte de Kafka para que
        // la escritura haya tenido éxito.
        // Esto podría dar lugar a una excepción que tenemos que manejar.
        // En este caso dejamos que suba hacia arriba y la manejamos en la clase OrderCreatedHandler.
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, orderDispatched).get();
    }
}
