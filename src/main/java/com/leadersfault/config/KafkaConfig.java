package com.leadersfault.config;

import com.leadersfault.dto.NotificationEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

  @Bean
  public ProducerFactory<String, NotificationEvent> producerFactory(
    KafkaProperties kafkaProperties
  ) {
    return new DefaultKafkaProducerFactory<>(
      kafkaProperties.buildProducerProperties()
    );
  }

  @Bean
  public KafkaTemplate<String, NotificationEvent> kafkaTemplate(
    ProducerFactory<String, NotificationEvent> producerFactory
  ) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public ConsumerFactory<String, NotificationEvent> consumerFactory(
    KafkaProperties kafkaProperties
  ) {
    return new DefaultKafkaConsumerFactory<>(
      kafkaProperties.buildConsumerProperties()
    );
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> kafkaListenerContainerFactory(
    ConsumerFactory<String, NotificationEvent> consumerFactory
  ) {
    ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
  }
}
