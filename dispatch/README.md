# dispatch

Hemos añadido un Consumer a nuestra aplicación.

## Notas

1. Generamos el proyecto en `https://start.spring.io/` usando como dependencias `Lombok` y `Spring for Apache Kafka`.

2. Añadimos un componente Spring Consumer al proyecto

`handler/OrderCreatedHandler.java`

Usamos la anotación `@KafkaListener` para hacerlo un Kafka Message Listener.

Spring Kafka consumirá los eventos de este topic por nosotros y los pasará a nuestro método listener.

También usaremos la anotación `@Component` para indicar que la clase es un Bean de Spring y la anotación `@Service` para indicar que la clase es un service.

Añadimos a `application.properties` configuración para deserialización. Con esto Spring sabrá que tipo de payload tendremos. Empezaremos con un tipo String.

Empezamos a construir un conjunto de pruebas unitarias, usando para ello `JUnit` y `Mockito`.

Ver documentación de Mockito:

`https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki/Mockito`

Por último, ejecutaremos la aplicación en la línea de comandos, usando la Kafka Command Line Tool Console Producer para enviar un evento `order.created` que lo consuma nuestra app.

3. JSON Deserializer

Actualizamos el consumer para deserializar el event payload a JSON.

Actualizamos el payload para que sea un JSON en vez de un String como vimos en el punto 2.

Documentación: `https://www.lydtechconsulting.com/blog-kafka-json-serialization.html`

Creamos un POJO Order que representará el payload y donde definimos los campos que requerimos en el event y que queremos consumir.

Pasamos este POJO al código en vez del String.

Cambiaremos `application.properties` para configurar este cambio, indicando el tipo por defecto del event (order).

Para deserializar un JSON vamos a necesitar la dependencia `Jackson`.

Ejecutaremos la aplicación en la terminal y, en otra terminal, el producer para enviar un event formateado como un JSON.

Veremos como la aplicación consume este evento.

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`
- Usaremos el CLI para enviar un evento order.created y ver como lo consume la aplicación
  - El kafka server son los contenedores docker de la Raspberry Pi
    - Confirmar que se están ejecutando y en caso contrario arrancarlos
  - Abrir una terminal con nombre `App` y ejecutar esta aplicación con el mandato siguiente:
    - `mvn spring-boot:run`
  - Abriremos otra terminal y le ponemos el nombre `producer`. Vamos a la carpeta donde esta la versión de Kafka que he instalado en el Mac
    - ~/Programacion/tools/kafka/kafka_2.13-3.9.0
    - Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092`
    - Indicamos un texto, por ejemplo `test-message` (esto ya no, era una primera versión de un commit anterior)
    - Indicamos un JSON de este tipo: `{"orderId":"e12a1993-5d30-4d0a-b28e-d849f9bbe9c4", "item":"item-1"}`
  - Volvemos a la terminal donde se está ejecutando nuestra aplicación y veremos en un log el mensaje enviado desde el producer y que hemos consumido
