package com.hugomalves.order;

import com.hugomalves.common.JSONSerde;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

import static com.hugomalves.common.Configurations.BOOTSTRAP_SERVERS;
import static com.hugomalves.common.Configurations.ORDERS_TOPIC;

public class OrderProducer {

  private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);


  public static void main(String[] args) {

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.StringSerde.class.getName());

    log.info("Creating new order");

    StringSerializer keySerializer = new StringSerializer();
    JSONSerde<OrderDto> valueSerde = new JSONSerde<>(OrderDto.class);
    KafkaProducer<String, OrderDto> producer = new KafkaProducer<>(props, keySerializer, valueSerde);

    OrderDto orderDto = new OrderDto();
    orderDto.addItem(new ProductDto(1L, "Iphone"));
    orderDto.addItem(new ProductDto(2L, "Samsung"));

    ProducerRecord<String, OrderDto> producerRecord = new ProducerRecord<>(ORDERS_TOPIC, orderDto.getId(), orderDto);
    Future<RecordMetadata> recordMetadataFuture = producer.send(producerRecord);
    try {
      recordMetadataFuture.get();
    } catch (Exception e) {
      log.error("Failed to send order event {} to kafka", orderDto.getId());
    }

    log.info("Successfully sent new order to the orders topic: {} ", orderDto);

    producer.close();

  }
}
