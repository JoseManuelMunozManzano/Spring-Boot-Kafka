# tracking-multiple-type-events

Vamos a actualizar la configuración del service tracking para que esté preparado y pueda recibir events de distintos tipos para el mismo topic. En concreto vamos a recibir el nuevo event `DispatchCompleted`.

![alt Multiple Events from Same Topic](../images/05-Multiple-Event-Types-From-Same-Topic.png)

Para poder hacer esta actualización, moveremos la anotación `@KafkaListener` desde el nivel de método al nivel de clase. En el método usaremos la anotación `@KafkaHandler`. Esta anotación aparecerá para cada tipo de event que el consumer pueda recibir.

También tendremos que eliminar el tipo de deserialización por defecto, el de `DispatchPreparing`, que habíamos indicado en la clase de Configuración. A cambio, Spring Kafka se basará en el type header que se incluye por defecto en los mensajes de Kafka producidor por un producer de Spring, como es el caso de nuestro Dispatch Service Producer.

Spring Kafka utiliza el tipo por defecto para identificar qué paquetes son de confianza, es decir, pueden ser deserializados, y tenemos que indicar dichos paquetes.

## Notas

1. Kafka Handler

Modificamos el fuente `DispatchTrackingHandler`, llevándonos la anotación `@KafkaListener` a nivel de clase.

En el método `listen` añadimos la anotación `@KafkaHandler`.

Podemos crear varios métodos listen() anotados con `@KafkaHandler`. Estos pueden ser sobrecargados o nombrarse de manera diferente.

Si recibimos un mensaje de este topic con un encabezado de tipo que no coincide con un método anotado con @KafkaHandler (un event desconocido de este topic), se lanzará un `ListenerExecutionFailedException` y continuará el polling del siguiente mensaje.

2. Trusted Packages

Spring Kafka necesita saber como deserializar cada event del array de bytes almacenado en Kafka a JSON y mapearlo a la representación Java del event.

Se modifica el fuente `TrackingConfiguration` para basarnos en la cabecera del mensaje que incluye el tipo de event, y Spring Kafka lo utilizará para seleccionar el tipo al que asignar el event deserializado.

Spring Kafka añade automáticamente esta cabecera de mensaje cuando produce el event, a menos que se configure explícitamente el no hacerlo.

Como los events que consume el Tracking Service se habrán originado en Dispatch Service, sabemos que esta cabecera estará presente. 

## Testing

- Clonar el repositorio
- Ejecutar el test de integración `TrackingStatusIntegrationTest.java`, método `testTrackingStatusFlow()`. 