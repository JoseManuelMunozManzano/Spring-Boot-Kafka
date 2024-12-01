# tracking

Hemos añadido un Consumer a nuestra aplicación.

Hemos añadido un Producer a nuestra aplicación.

Hemos añadido Test de Integración.

## Notas

1. Test de integración

Añadimos tests de integración, es decir, cargando el contexto de aplicación de Spring `@SpringBootTest`, instanciando los beans de Spring e integrándolo con Kafka.

En vez de usar una instancia externa de Kafka, utilizaremos el broker de Kafka embebido en Spring Kafka.

Ver en el package de tests, `integration/TrackingStatusIntegrationTest.java`. También se crea `resources/application-test.properties`.

Hacemos uso de la librería de pruebas `awaitility` que nos permite esperar a que una condición se cumpla en un periodo de tiempo determinado.

Es muy útil para probar flujos de mensajería asíncrona.

```
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.2</version>
    <scope>test</scope>
</dependency>
```

Indicando el scope a test, la librería no se incluye en el archivo .jar al hacer el build.

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

- Para probar el test de integración situado en `integration/TrackingStatusIntegrationTest.java` ejecutar:
  - `mvn clean install`