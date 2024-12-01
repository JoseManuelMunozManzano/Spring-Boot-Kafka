# dispatch-ordering

Creamos el proyecto `dispatch-ordering` al que se le pasa una message key al handler.

Para ello utilizaremos la anotación de Spring Kafka `@Header` y, en concreto usaremos `KafkaHeaders.RECEIVED_KEY`. Kafka extraerá la key del mensaje y la pasará al handler por nosotros.

Haremos lo mismo con `KafkaHeaders.RECEIVED_PARTITION`. Esto nos permitirá registrar la partition en la que se recibe cada event para verificar que los mensajes con la misma key se reciben en la misma partition.

Demostraremos que los mensajes producidos con la misma key serán escritos a, y consumidos desde la misma partition.

## Notas

1. Para añadir ordering hemos tocado tanto el handler, método `listen()` como nuestro service, método `process()`

Actualizamos los tests unitarios y de integración.

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`

- Ejecutar una instancia de la aplicación, en una terminal a la que ponemos el nombre `App_1`: `mvn spring-boot:run`

- Abriremos una terminal y le ponemos el nombre `consumer`. Vamos a la carpeta donde está la versión de Kafka que he instalado en el Mac
  - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar `bin/kafka-console-consumer.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --topic order.dispatched --property print.key=true --property key.separator=:`

- Abriremos una terminal y le ponemos el nombre `producer`. Vamos a la carpeta donde está la versión de Kafka que he instalado en el Mac
  - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --property parse.key=true --property key.separator=:`
  - Introducimos una key que es un String seguido de un JSON de este tipo: `"123":{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-10"}`

- En los logs de la aplicación, terminal con nombre `App_1` veremos: `Received message: partition: 0 - key: "123" - payload: OrderCreated(orderId=e12a1993-5d30-4d0a-b28e-d849f9bbe9c4, item=item-10)`
- En el consumer veremos también la key

**DEMOSTRACIÓN MISMAS CLAVES->MISMO TOPIC PARTITION**

- Cancelamos la ejecución de nuestra aplicación.
- Cancelamos la ejecución de nuestro `producer` y vamos a hacer que tenga 5 partitions.
- Comenzamos viendo las partitions que hay asociadas a mi topic `order.created`
  - Ejecutar `bin/kafka-topics.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --describe --topic order.created`
    - Vemos que PartitionCount vale 1 y solo tenemos la Partition 0
- Creamos las 5 partitions
  - Ejecutar `bin/kafka-topics.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --alter --topic order.created --partitions 5`
- Si volvemos a ejecutar el mandato `bin/kafka-topics.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --describe --topic order.created`
  - Vemos que PartitionCount vale 5 y tenemos Partition 0 hasta 4
- Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --property parse.key=true --property key.separator=:`
- Ejecutar nuestra aplicación en la terminal `App_1`: `mvn spring-boot:run`
  - Veremos que tiene asignadas las 5 partitions
- Ejecutar una nueva instancia de nuestra aplicación en una terminal `App_2`: `mvn spring-boot:run`
  - Será el mismo consumer group
- Vemos ahora como para la instancia de la terminal `App_1` tenemos asignadas las partitions 0, 1 y 2 y para la instancia de la terminal `App_2` tenemos asignadas las partitions 3 y 4.
  - Se ha rebalanceado
- Podemos ver más información sobre las partitions utilizando la siguiente herramienta de línea de comandos de Kafka.
  - En una nueva terminal, acceder a la ruta `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar: `bin/kafka-consumer-groups.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --list`
  - Y ahora, usando el consumer group que nos interese, ejecutar: `bin/kafka-consumer-groups.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --describe --group dispatch.order.created.consumer`


- En la terminal `producer`, introducimos una key que es un String seguido de un JSON de este tipo: `"456":{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-11"}`
  - Veo que se ha consumido en la instancia de la terminal `App_2`, en la partition 3
    -  Received message: partition: 3 - key: "456" - payload: OrderCreated(orderId=e12a1993-5d30-4d0a-b28e-d849f9bbe9c4, item=item-11)
  - En la instancia de la terminal `App_1` no hay nada
- Si ahora vuelvo a enviar la misma key en `producer`, tiene que irse a la misma partition: `"456":{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-12"}` 
  - Y funciona como se espera, va a la partition 3
- Y ejecuto en `producer`: `"456":{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-13"}`
  - Y de nuevo va a la partition 3

- Si ahora paramos el `producer` y ejecutamos de nuevo para ver el detalle de las partitions
  - `bin/kafka-consumer-groups.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --describe --group dispatch.order.created.consumer`
  - Veremos en la partition 3 que se han consumido los 3 events que he mandado por el producer


- Volvemos a ejecutar el `producer`: `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --property parse.key=true --property key.separator=:`
  - Enviamos otros dos event, pero con otra key distinta:
    - `"789":{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-14"}`
    - `"789":{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-15"}`
  - En este caso, se han consumido usando la partition 0 