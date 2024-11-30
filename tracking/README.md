# tracking

Hemos añadido un Consumer a nuestra aplicación.

Hemos añadido un Producer a nuestra aplicación.

## Notas

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`

- Usaremos el CLI para enviar un evento `order.created`, ver como lo consume la aplicación y produce un `tracking.status` que va a un consumer del CLI
  - El kafka server son los contenedores docker de la Raspberry Pi
    - Confirmar que se están ejecutando y en caso contrario arrancarlos
  - Abrir una terminal con nombre `Dispatch` y ejecutar la aplicación `dispatch` con el mandato siguiente:
    - `mvn spring-boot:run`
  - Abrir una terminal con nombre `Tracking` y ejecutar esta aplicación con el mandato siguiente:
    - `mvn spring-boot:run`
    
  - Abriremos una terminal y le ponemos el nombre `consumer`. Vamos a la carpeta donde esta la versión de Kafka que he instalado en el Mac
    - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
    - Ejecutar `bin/kafka-console-consumer.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --topic tracking.status`

  - Abriremos otra terminal y le ponemos el nombre `producer`. Vamos a la carpeta donde esta la versión de Kafka que he instalado en el Mac
    - ~/Programacion/tools/kafka/kafka_2.13-3.9.0
    - Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092`
    - Indicamos un JSON de este tipo: `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-1"}`

  - Volvemos a la terminal donde se está ejecutando nuestra aplicación y veremos en un log el siguiente mensaje enviado desde el producer del CLI y que hemos consumido
    - `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "status":"PREPARING"}`
  - Y si vamos al CLI del `consumer` veremos que se ha consumido ahí también el mensaje enviado desde el producer de nuestra aplicación
