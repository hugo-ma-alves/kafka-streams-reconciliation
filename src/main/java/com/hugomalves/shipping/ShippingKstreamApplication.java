package com.hugomalves.shipping;

import com.hugomalves.common.JSONSerde;
import com.hugomalves.manufacturer.ProductManufacturedDto;
import com.hugomalves.order.OrderDto;
import com.hugomalves.shipping.processor.ManufacturerProcessor;
import com.hugomalves.shipping.processor.OrderProcessor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.hugomalves.common.Configurations.*;

public class ShippingKstreamApplication {

  private static final Logger log = LoggerFactory.getLogger(ShippingKstreamApplication.class);

  public static final String ORDER_PROCESSOR_NAME = "Order-Processor";
  public static final String MANUFACTURED_PROCESSOR_NAME = "Manufactured-Processor";

  private static final String SHIPPING_STATE_STORE_NAME = "SHIPPING_STATE_STORE";

  public Topology buildTopology() {
    StreamsBuilder streamsBuilder = new StreamsBuilder();

    //1) Configuration - set up the serializers and deserializers
    Serdes.StringSerde keySerde = new Serdes.StringSerde();
    JSONSerde<OrderDto> orderSerde = new JSONSerde<>(OrderDto.class);
    JSONSerde<ProductManufacturedDto> manufacturedSerde = new JSONSerde<>(ProductManufacturedDto.class);
    JSONSerde<ShippingDto> shippingDtoSerde = new JSONSerde<>(ShippingDto.class);
    JSONSerde<OrderManufacturingStatus> stateStoreSerde = new JSONSerde<>(OrderManufacturingStatus.class);


    //2) Attach the state store to the stream
    StoreBuilder<KeyValueStore<String, OrderManufacturingStatus>> stateStore = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(SHIPPING_STATE_STORE_NAME),
            Serdes.String(), stateStoreSerde);
    streamsBuilder.addStateStore(stateStore);

    //3) Stream the orders topic and for each record apply the OrderProcessor
    KStream<String, ShippingDto> ordersStream = streamsBuilder.stream(ORDERS_TOPIC, Consumed.with(keySerde, orderSerde))
                                                              .process(OrderProcessor::new,
                                                                       Named.as(ORDER_PROCESSOR_NAME),
                                                                       SHIPPING_STATE_STORE_NAME);

    //4) Stream the products manufactured topic and for each record apply the ManufacturerProcessor
    KStream<String, ShippingDto> manufacturingStream = streamsBuilder.stream(
                                                                             MANUFACTURED_TOPIC, Consumed.with(keySerde, manufacturedSerde))
                                                                     .process(ManufacturerProcessor::new,
                                                                              Named.as(MANUFACTURED_PROCESSOR_NAME),
                                                                              SHIPPING_STATE_STORE_NAME);

    //5) Merge both topics, and forward the event emitted by any of the processors to the shipping topic
    ordersStream.merge(manufacturingStream)
                .to(SHIPPING_TOPIC, Produced.with(keySerde, shippingDtoSerde));

    Topology topology = streamsBuilder.build();
    log.debug(topology.describe().toString());

    return topology;
  }

  public static void main(String[] args) {

    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "shipping-reconciliation-app");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());

    ShippingKstreamApplication shippingKstreamApplication = new ShippingKstreamApplication();
    Topology topology = shippingKstreamApplication.buildTopology();

    KafkaStreams kafkaStreams = new KafkaStreams(topology, props);

    Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

    kafkaStreams.start();
  }
}