# dispatch-shared-consumer-group

Ejecutaremos dos instancias de esta aplicación.

La aplicación se actualizará para generar un ID de aplicación único al iniciarse. Cada instancia tendrá un ID único y lo añadiremos al evento de salida order.dispatched, para que podamos ver desde qué instancia se consumió el event order.created y se produjo el event order.dispatched.

Como cada aplicación tiene un consumer definido escuchando el topic order.created, en el método handler, anotación @KafkaListener, especificando el consumer group `dispatch.order.created.consumer`, ambas instancias se iniciarán en el mismo grupo de consumers.

El topic tiene una sola partition y se asignará una única instancia de consumidor de aplicaciones. Por lo tanto, solo una instancia de consumer consumirá el event y solo veremos un event de order.dispatched.

Utilizaremos una terminal como producer y otra como consumer para demostrar este comportamiento.

## Notas

1. ID de aplicación distinto

Hecho en `DispatchService.java`: `private static final UUID APPLICATION_ID = randomUUID();`

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`

- Ejecutar dos instancias de la aplicación
  - Primera ejecución en una terminal a la que ponemos el nombre `App_1`: `mvn spring-boot:run`
  - Segunda ejecución en una terminal a la que ponemos el nombre `App_2`: `mvn spring-boot:run`
  - Veo lo siguiente:
    - App_1: `dispatch.order.created.consumer: partitions assigned: []`
    - App_2: `dispatch.order.created.consumer: partitions assigned: [order.created-0]`

- Abriremos una terminal y le ponemos el nombre `consumer`. Vamos a la carpeta donde está la versión de Kafka que he instalado en el Mac
  - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar `bin/kafka-console-consumer.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --topic order.dispatched`

- Abriremos otra terminal y le ponemos el nombre `producer`. Vamos a la carpeta donde está la versión de Kafka que he instalado en el Mac
  - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092`
  - Indicamos un JSON de este tipo: `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-5"}`

- Vamos a la terminales `App_1` y `App_2` y veremos que en una de ellas, en mi caso `App_2` se le ha asignado la partición y veo:
  - `Received message: payload: OrderCreated(orderId=e12a1993-5d30-4d0a-b28e-d849f9bbe9c4, item=item-5)`
  - `App_1` ha quedado inactiva, lo que es correcto
  
- Vamos a la terminal `consumer` y veremos:
  - `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4","processedById":"e2d9b50d-a5ff-4b7a-b932-de07fcfbb0d8","notes":"Dispatched: item-5"}`