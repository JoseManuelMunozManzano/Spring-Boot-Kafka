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

**SHARED CONSUMER GROUP**

- Vamos a la terminales `App_1` y `App_2` y veremos que en una de ellas, en mi caso `App_2` se le ha asignado la partición y veo:
  - `Received message: payload: OrderCreated(orderId=e12a1993-5d30-4d0a-b28e-d849f9bbe9c4, item=item-5)`
  - `App_1` ha quedado inactiva, lo que es correcto
  
- Vamos a la terminal `consumer` y veremos:
  - `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4","processedById":"e2d9b50d-a5ff-4b7a-b932-de07fcfbb0d8","notes":"Dispatched: item-5"}`

**PRUEBA DE FAILSAFE**

- Detenemos la aplicación (Ctrl+C) que tiene asignado el topic partition, es decir, en la que aparezca el texto `order.created`, tal que así:
  - `dispatch.order.created.consumer: partitions assigned: [order.created-0]`
- Veremos que la aplicación que sigue arrancada y tenía este texto `dispatch.order.created.consumer: partitions assigned: []` ahora tiene este texto:
  - `dispatch.order.created.consumer: partitions assigned: [order.created-0]`
- Si ahora lanzamos un segundo event en la terminal `producer`:
  - `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-6"}`
- Veremos que en la única terminal de la aplicación en ejecución se le ha asignado la partición.

**DUPLICATE CONSUMPTION**

- Empezamos con las dos instancias de la aplicación, terminales con nombres `App_1` y `App_2`, sin ejecutar
- Ejecutar la app en la terminal con nombre `App_1`: `mvn spring-boot:run`
- Ir al fuente `handler/OrderCreatedHandler` y cambiar el groupId de la anotación `@KafkaListener`, por ejemplo a: `groupId = "dispatch.order.created.consumer-2",`
  - Con esto conseguimos dos consumer group distintos
- Ejecutar la app en la terminal con nombre `App_2`: `mvn spring-boot:run`
- En ambas terminales veremos: `dispatch.order.created.consumer: partitions assigned: [order.created-0]`
- Si ahora lanzamos un tercer event en la terminal `producer`:
- `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-7"}`
- Tanto en la terminal `App_1` como en `App_2` veremos que se ha asignado la partición, es decir, el mensaje se ha procesado en ambas instancias de la aplicación
  - Si nos fijamos en el log, en `processedById` veremos que son distintos
- Y veremos en la terminal del `consumer` que aparece dos veces el event `order.dispatched`
