# tracking

Hemos añadido un Consumer a nuestra aplicación.

Hemos añadido un Producer a nuestra aplicación.

## Notas

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`

- Usaremos el CLI para enviar un evento `order.created`, ver como lo consume la aplicación y produce un `order.dispatched` que va a un consumer del CLI
  - El kafka server son los contenedores docker de la Raspberry Pi
    - Confirmar que se están ejecutando y en caso contrario arrancarlos
  - Abrir una terminal con nombre `App` y ejecutar esta aplicación con el mandato siguiente:
    - `mvn spring-boot:run`
    
  - Abriremos una terminal y le ponemos el nombre `consumer`. Vamos a la carpeta donde esta la versión de Kafka que he instalado en el Mac
    - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
    - Ejecutar `bin/kafka-console-consumer.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --topic order.dispatched`

  - Abriremos otra terminal y le ponemos el nombre `producer`. Vamos a la carpeta donde esta la versión de Kafka que he instalado en el Mac
    - ~/Programacion/tools/kafka/kafka_2.13-3.9.0
    - Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092`
    - Indicamos un texto, por ejemplo `test-message` (esto ya no, era una primera versión de un commit anterior)
    - Indicamos un JSON de este tipo: `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-1"}`

  - Volvemos a la terminal donde se está ejecutando nuestra aplicación y veremos en un log el mensaje enviado desde el producer del CLI y que hemos consumido
  - Y si vamos al CLI del `consumer` veremos que se ha consumido ahí también el mensaje enviado desde el producer de nuestra aplicación

- Para probar gestión de errores, tenemos que enviar desde el `producer` un JSON inválido
  - Enviar un JSON de este tipo: `{"orderId":"123", "item":"invalid-1"}`
- Al volver a la terminal donde está siendo ejecutada nuestra app, veremos un bucle infinito porque Spring no puede deserializar este evento y lanza excepciones sin parar
  - La excepción es `InvalidFormatException`
- Como ha ocurrido una excepción, el evento se vuelve a reenviar inmediatamente en el siguiente poll del consumer, lo que provoca otra excepción, así de forma infinita
- Al modificar `application.properties` el error solo se da una vez, no como bucle infinito, y permite consumir el siguiente evento
- Y, en las notas (la 5), al cambiar de application.properties al archivo de configuración de Spring, también está controlado

