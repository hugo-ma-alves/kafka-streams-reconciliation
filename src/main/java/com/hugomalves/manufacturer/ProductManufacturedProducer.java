package com.hugomalves.manufacturer;


import com.hugomalves.common.JSONSerde;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.Future;

import static com.hugomalves.common.Configurations.BOOTSTRAP_SERVERS;
import static com.hugomalves.common.Configurations.MANUFACTURED_TOPIC;

public class ProductManufacturedProducer {

  private static final Logger log = LoggerFactory.getLogger(ProductManufacturedProducer.class);

  public static void main(String[] args) {

    String orderId = args[0];
    Long productId = Long.valueOf(args[1]);

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.StringSerde.class.getName());

    StringSerializer keySerializer = new StringSerializer();
    JSONSerde<ProductManufacturedDto> valueSerde = new JSONSerde<>(ProductManufacturedDto.class);

    Producer<String, ProductManufacturedDto> producer = new KafkaProducer<>(props, keySerializer, valueSerde);

    log.info("Creating new product manufactured event");

    ProductManufacturedDto productManufacturedDto = new ProductManufacturedDto(orderId, productId,
                                                                               LocalDateTime.now());

    var producerRecord = new ProducerRecord<>(MANUFACTURED_TOPIC, orderId, productManufacturedDto);

    Future<RecordMetadata> recordMetadata = producer.send(producerRecord);
    try {
      recordMetadata.get();
    } catch (Exception e) {
      log.error("Failed to send manufactured event {} to kafka", productManufacturedDto.getProductId());
    }

    log.info("Successfully sent new manufactured event to kafka: {} ", productManufacturedDto);

    producer.close();
  }

}
