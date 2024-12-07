# dispatch-error-handling

Para demostrar el comportamiento de reintento, se requiere un mecanismo que desencadene excepciones, que serán lanzadas por la aplicación al recibir un event.

Paa facilitar esto, vamos a añadir una llamada externa a un nuevo servicio llamado `Stock Service`.

Para comprobar la disponibilidad del item que se está pidiendo, el service no se implantará realmente. En su lugar, usaremos Wiremock para simular las responses que se devolverán en función del REST request entrante que reciba, incluidas las responses que harán que se vuelva a intentar el event.

Definiremos un backoff fijo con un máximo especificado de reintentos y el intervalo entre cada reintento.

También especificaremos qué excepciones reintentar y cuáles no.

Añadiremos la llamada a `Stock Service`, que se dispara al procesar cada event `order.created` que consume la aplicación y actualizaremos nuestro Exception handler para lanzar una excepción recuperable cuando se captura un error transitorio. En nuestro caso, un error de servidor devuelto por `Stock Service`, como una response de Bad Gateway o servicio no disponible.

Luego actualizaremos los tests de integración para comprobar el comportamiento de reintento. Anotamos el test con la anotación `@AutoConfigureWiremock` y utilizamos la placeholder property `wiremock.server.port` en la URL del endpoint de `Stock Service` en las propiedades de los tests de la aplicación que permiten sustituir el puerto dinámico en tiempo de ejecución.

Por último, vamos a ejecutar la aplicación Wiremock independiente `Stock Service` usando la línea de comandos. Como ahora `Dispatch Service` hace una llamada a `Stock Service`, necesitamos esto en su lugar, con el fin de ejecutar el flujo completo de extremo a extremo.

El repositorio Git con el Wiremock independiente es: `https://github.com/lydtechconsulting/introduction-to-kafka-wiremock`.

## Notas

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`

**Retry: Commandline Demo**

- Abriremos una terminal y le ponemos el nombre `wiremock`. Accedemos al proyecto `introduction-to-kafka-wiremock` y bajamos el jar de `https://wiremock.org/docs/download-and-installation/`
  - Ejecutamos el jar de Wiremock: `java -jar wiremock-standalone-3.10.0.jar --port 9001`
  - El puerto es el especificado en `application.properties`
  - Wiremock se ejecutará e incluirá los mapeos stub definidos en el repositorio Wiremock

- Enviaremos tres eventos diferentes que desencadenan diferentes respuestas de Wiremock y observaremos el comportamiento del reintento resultante

- Recordar que en la RaspberryPi debe estar ejecutándose el server de Kafka

- Ejecutar una instancia de la aplicación `dispatch-error-handling`, en una terminal a la que ponemos el nombre `dispatch`: `mvn spring-boot:run`

- Abriremos una terminal y le ponemos el nombre `consumer`. Vamos a la carpeta donde está la versión de Kafka que he instalado en el Mac
  - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar `bin/kafka-console-consumer.sh --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --topic order.dispatched --property print.key=true --property key.separator=:`

- Abriremos una terminal y le ponemos el nombre `producer`. Vamos a la carpeta donde está la versión de Kafka que he instalado en el Mac
  - `~/Programacion/tools/kafka/kafka_2.13-3.9.0`
  - Ejecutar `bin/kafka-console-producer.sh --topic order.created --bootstrap-server 192.168.1.41:29092,192.168.1.41:39092,192.168.1.41:49092 --property parse.key=true --property key.separator=:`

- Empezamos escribiendo en el `producer` un event que resulta en un éxito de 200 de Wiremock
  - `"200":{"orderId": "3a814e68-337c-421b-a294-8397fbace710", "item": "item_200"}`
  - Este es el comportamiento por defecto si Wiremock no encuentra otro match en el nombre del item
  - El nombre del item, item_200 refleja la response 200 esperada de Wiremock
  - Si vamos a la terminal `consumer`, veremos el event consumido:
    - `"200":{"orderId":"3a814e68-337c-421b-a294-8397fbace710","processedById":"8c596663-eea5-4a02-b5bf-140fc1b34258","notes":"Dispatched: item_200"`
  - Si vamos a los logs de la aplicación en la terminal `dispatch` vemos:
    - `Received message: partition: 0 - key: "200" - orderId: 3a814e68-337c-421b-a294-8397fbace710 - item: item_200`
    - `Sent messages: key: "200" - orderId: 3a814e68-337c-421b-a294-8397fbace710 - processedById: 8c596663-eea5-4a02-b5bf-140fc1b34258`
    - No hay errores ni reintentos del mensaje

- Seguimos escribiendo en el `producer` un event con error 400, que no será reintentado porque es un error en la petición
  - `"400":{"orderId": "3a814e68-337c-421b-a294-8397fbace710", "item": "item_400"}`
  - Es el sufijo del item, en este caso item_400 el que dispara que Wiremock devuelva el status 400
  - Si vamos a la terminal `consumer` vemos que no hay evento, lo que es correcto
  - Si vamos a la terminal `dispatch` vemos el evento recibido, la excepción y el backoff
    - `Received message: partition: 1 - key: "400" - orderId: 3a814e68-337c-421b-a294-8397fbace710 - item: item_400`
    - `NotRetryable exception: 400 Bad Request on GET request for "http://localhost:9001/api/stock": [no body]`
    - `Backoff FixedBackOff{interval=0, currentAttempts=1, maxAttempts=0} exhausted for order.created-1@0`

- Nuestra última prueba en el `producer` es un event con error 502 (Bad Gateway), que será reintentado
  - `"502":{"orderId": "3a814e68-337c-421b-a294-8397fbace710", "item": "item_502"}`
  - Si vamos a la terminal `consumer`, veremos el event consumido:
    - `"502":{"orderId":"3a814e68-337c-421b-a294-8397fbace710","processedById":"8c596663-eea5-4a02-b5bf-140fc1b34258","notes":"Dispatched: item_502"}`
  - Si vamos a la terminal `dispatch` vemos el evento recibido, la excepción y el reintento
    - `Received message: partition: 3 - key: "502" - orderId: 3a814e68-337c-421b-a294-8397fbace710 - item: item_502`
    - `Retryable exception: org.springframework.web.client.HttpServerErrorException$BadGateway: 502 Bad Gateway on GET request for "http://localhost:9001/api/stock": [no body]`
    - `Received message: partition: 3 - key: "502" - orderId: 3a814e68-337c-421b-a294-8397fbace710 - item: item_502`
    - `Sent messages: key: "502" - orderId: 3a814e68-337c-421b-a294-8397fbace710 - processedById: 8c596663-eea5-4a02-b5bf-140fc1b34258`