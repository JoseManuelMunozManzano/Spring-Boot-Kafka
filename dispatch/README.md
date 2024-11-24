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

`spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer`

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

`spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer`

`spring.kafka.consumer.properties.spring.json.value.default.type=com.jmmm.dispatch.message.OrderCreated`

Para deserializar un JSON vamos a necesitar la dependencia `Jackson`.

Ejecutaremos la aplicación en la terminal y, en otra terminal, el producer para enviar un event formateado como un JSON.

Veremos como la aplicación consume este evento.

4. Deserializer Error Handling

Vamos a ver como manejar los errores de deserialización JSON cuando este no es válido.

Documentación:

`https://www.lydtechconsulting.com/blog-kafka-poison-pill.html`

Hacemos la demostración enviando un JSON inválido en la línea de comandos del producer y observando su comportamiento.

Luego, abordamos la gestión de errores actualizando la configuración de `application.properties`, para usar gestión de errores de Spring Kafka y configurándolo para delegar en el deserializador JSON.

`spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer`

`spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer`

5. Spring Bean Configuration

Vamos a definir la configuración de deserialización programáticamente con Spring Beans.

Hasta ahora habíamos definido las propiedades de deserialización en el archivo `application.properties`.

Esto lo cambiamos para definir Beans de Spring explícitamente.

Vamos a definir un `KafkaListenerContainerFactory` que utilizará Spring para construir el contenedor para nuestro `OrderCreatedHandler` anotado con `KafkaListener`.

Le pasaremos un `ConsumerFactory`, que define la estrategia para crear la instancia del consumer.

Y configuraremos el `ConsumerFactory` con las clases de deserialización que Spring utilizará para el proceso de deserialización.

La ventaja de todo esto es:
- Podemos definir más fácilmente distintos beans para distintos escenarios, por ejemplo, diferentes listeners que tienen diferentes timeouts definidos
- Nos da la confirmación, en tiempo de compilación, que las clases están correctamente definidas y en el classpath de la aplicación

6. Create the Topics

Comparamos la creación automática de topics con la manual.

En este momento, nuestra aplicación está consumiento events de un topic llamado order.created

No hemos tenido que crear manualmente este topic, y esto es debido a que no hemos anulado las configuraciones por defecto en el broker y en el consumer.

En tiempo de desarrollo de la aplicación esto es una ventaja. Lo veremos también cuando lleguemos a ejecutar pruebas de integración en una sección posterior.

Sin embargo, es posible crear topics incorrectos. Por ejemplo, un equipo puede entender que el topic debe llamarse order.updated en vez de order.created, y este topic order.updated se crearía por error cuando el producer enviara un event, aunque ninguna aplicación estaría esparando para consurmirlo.

Como decimos, el broker está configurado con el parámetro `auto.create.topics` con valor a `true` por defecto.

Y el consumer está configurado con el parámetro `allow.auto.create.topics` con valor a `true` por defecto.

Si indicamos el valor del parámetro del broker a false, forzamos el requisito de que el topic se cree manualmente antes de que se pueda utilizar.

Si indicamos el valor del parámetro del consumer a false, el consumer no podrá hacer polling de un topic que aún no se ha creado.

Para producción la mejor práctica indica que los topic deben crearse manualmente, usando el CLI.

Por supuesto, deberíamos hacer testing del pipeline que crea la infraestructura, incluyendo los topics, en otros entornos remotos como QA y Sandbox antes de ejecutarlo en producción, para confirmar que no permite usar topics que no se han creado manualmente.

7. Produce

Vamos a introducir un `producer` Kafka en la aplicación.

El fuente `DispatchService.java` tiene el método `process()` que actualmente está marcado con `todo`.

Lo actualizamos para enviar un JSON event de salida al topic `order.dispatched`.

Cada vez que se consuma un event order.created, ahora vamos a emitir un nuevo event order.dispatched

```
                  order.created                      order.dispatched
console producer ------------------->   dispatch ------------------------>
```

Los eventos serán representados por un POJO y utilizaremos la clase KafkaTemplate de Spring Kafka para enviar el event.

KafkaTemplate proporciona una abstracción sobre las API de cliente Kafka de bajo nivel para enviar y recibir eventos, haciendo que la tarea de enviar un evento sea sencilla para el desarrollador.

Definiremos KafkaTemplate String Bean en nuestra clase de configuración y conectaremos a esta la fábrica producer que usaremos para configurar las propiedades de serialización de events para serializar el event a JSON.

Por defecto, KafkaTemplate envía mensajes de forma asíncrona (dispara y olvida) El envío podría fallar, pero el service no sería consciente de ello.

Por tanto, haremos que la llamada sea síncrona. Con esto, podemos ver si el event se escribe o no en el broker y, si falla, podemos lanzar una excepción que debe ser manejada en la aplicación, junto a su prueba unitaria.

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`

- Usaremos el CLI para enviar un evento `order.created` y ver como lo consume la aplicación
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

  - Para probar gestión de errores, tenemos que enviar desde el `producer` un JSON inválido
    - Enviar un JSON de este tipo: `{"orderId":"123", "item":"invalid-1"}`
  - Al volver a la terminal donde está siendo ejecutada nuestra app, veremos un bucle infinito porque Spring no puede deserializar este evento y lanza excepciones sin parar
    - La excepción es `InvalidFormatException`
  - Como ha ocurrido una excepción, el evento se vuelve a reenviar inmediatamente en el siguiente poll del consumer, lo que provoca otra excepción, así de forma infinita
  - Al modificar `application.properties` el error solo se da una vez, no como bucle infinito, y permite consumir el siguiente evento
  - Y, en las notas (la 5), al cambiar de application.properties al archivo de configuración de Spring, también está controlado
