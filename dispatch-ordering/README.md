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