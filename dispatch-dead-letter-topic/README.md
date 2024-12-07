# dispatch-dead-letter-topic

Veremos como enviar events al Dead Letter Topic para excepciones de no recuperación y para cuando se han agotado los intentos de reintento.

En este módulo creamos la app `dispatch-dead-letter-topic` para decirle que envíe los events al Dead Letter Topic usando Spring Kafka, en concreto `DeadLetterPublishingRecoverer`.

El nombre de la Dead Letter Topic es, por defecto, el nombre del topic original acabado en `-dlt`. Por ejemplo, para `order.created` será `order.created-dlt`.

Actualizaremos los integrations tests para probar que los events que han dado lugar a que se lance una excepción no reintentada y los que han sido reintentados pero han agotado los intentos, se envían al Dead Letter Topic.

## Notas

## Testing

- Clonar el repositorio
- Construcción y testing de la aplicación (esto cada vez que se haga cualquier cambio en la app)
  - `mvn clean install`