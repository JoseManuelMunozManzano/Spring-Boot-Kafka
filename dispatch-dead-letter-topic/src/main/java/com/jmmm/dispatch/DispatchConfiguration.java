package com.jmmm.dispatch;

import java.util.HashMap;
import java.util.Map;

import com.jmmm.dispatch.exception.NotRetryableException;
import com.jmmm.dispatch.exception.RetryableException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.jmmm.dispatch.message.OrderCreated;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.client.RestTemplate;

// Con @Configuration, Spring cargará estos beans en el contexto de la aplicación.
// Con @ComponentScan indicamos a Spring donde encontrar otros componentes Spring definidos.
@Configuration
@ComponentScan(basePackages = {"com.jmmm"})
public class DispatchConfiguration {

  private static String TRUSTED_PACKAGES = "com.jmmm.dispatch.message";

  // Este es nuestro KafkaListenerContainerFactory.
  // Aunque se pueden usar distintos tipos esta es la implementación recomendada.
  // Actuará como la factory para nuestro consumer Kafka OrderCreatedHandler.
  // Por ahora indicamos como Key String y, aunque el consumer está consumiendo un
  // tipo OrderCreated, vamos a utilizar un valor genérico Object.
  // El objetivo de usar Object, es reutilizar este bean con cualquier consumer,
  // sin importar el tipo de evento
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      ConsumerFactory<String, Object> consumerFactory, KafkaTemplate<String, Object> kafkaTemplate) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    // Añadimos Dead Letter Topic. Por defecto, el nombre de la Dead Letter Topic será el nombre del topic original acabado en -dlt
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(100L, 3L));
    errorHandler.addRetryableExceptions(RetryableException.class);
    errorHandler.addNotRetryableExceptions(NotRetryableException.class);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  // En el ConsumerFactory especificamos las propiedades que necesitamos.
  // Estas propiedades son las mismas que pusimos en su momento en application.properties.
  // El server de Kafka seguirá en application.properties (con otro nombre, sin el prefijo spring)
  // porque probablemente será cambiado para cada entorno donde se despliegue este proyecto.
  @Bean
  public ConsumerFactory<String, Object> consumerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
    config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreated.class.getCanonicalName());
    config.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
    // Este deserializador de String lo usaremos para la key del mensaje. Es para más adelante.
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    return new DefaultKafkaConsumerFactory<>(config);
  }

  // Producer
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public ProducerFactory<String, Object> producerFactory(@Value("${kafka.bootstrap-servers}")  String bootstrapServers) {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
  
}
