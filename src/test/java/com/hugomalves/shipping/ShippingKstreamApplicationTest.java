package com.hugomalves.shipping;

import com.hugomalves.common.JSONSerde;
import com.hugomalves.manufacturer.ProductManufacturedDto;
import com.hugomalves.order.OrderDto;
import com.hugomalves.order.ProductDto;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.hugomalves.common.Configurations.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class ShippingKstreamApplicationTest {

  private TopologyTestDriver testDriver;

  private TestInputTopic<String, OrderDto> ordersTopic;
  private TestInputTopic<String, ProductManufacturedDto> productManufacturedTopic;
  private TestOutputTopic<String, ShippingDto> shippingTopic;

  private final ShippingKstreamApplication shippingKstreamApplication = new ShippingKstreamApplication();

  @BeforeEach
  public void setup() {

    Properties props = new Properties();
    props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "shipping-reconciliation-app-test");
    props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
    props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

    //Topology being tested
    Topology topology = shippingKstreamApplication.buildTopology();

    testDriver = new TopologyTestDriver(topology, props);

    Serdes.StringSerde keySerde = new Serdes.StringSerde();
    JSONSerde<OrderDto> orderSerde = new JSONSerde<>(OrderDto.class);
    JSONSerde<ProductManufacturedDto> manufacturedSerde = new JSONSerde<>(ProductManufacturedDto.class);
    JSONSerde<ShippingDto> shippingDtoSerde = new JSONSerde<>(ShippingDto.class);

    ordersTopic = testDriver.createInputTopic(ORDERS_TOPIC, keySerde.serializer(), orderSerde);
    productManufacturedTopic = testDriver.createInputTopic(MANUFACTURED_TOPIC, keySerde.serializer(),
                                                           manufacturedSerde);
    shippingTopic = testDriver.createOutputTopic(SHIPPING_TOPIC, keySerde.deserializer(), shippingDtoSerde);

  }

  @AfterEach
  public void tearDown() {
    testDriver.close();
  }

  @Test
  void order_shipped_after_manufactured() {
    ProductDto product1 = new ProductDto(1L, "Product 1");
    ProductDto product2 = new ProductDto(2L, "Product 2");
    OrderDto orderDto = new OrderDto(Set.of(product1, product2));

    String orderId = "bc35d5f1-c3c1-4d4d-9a04-89f6e76f29cd";

    ordersTopic.pipeInput(orderId, orderDto);

    ProductManufacturedDto product1Manufactured = new ProductManufacturedDto(orderId, 1L, LocalDateTime.now());
    ProductManufacturedDto product2Manufactured = new ProductManufacturedDto(orderId, 2L, LocalDateTime.now());
    productManufacturedTopic.pipeInput(orderId, product1Manufactured);
    productManufacturedTopic.pipeInput(orderId, product2Manufactured);

    List<TestRecord<String, ShippingDto>> outputRecords = shippingTopic.readRecordsToList();
    assertThat(outputRecords.size(), is(1));

    TestRecord<String, ShippingDto> shippingRecord = outputRecords.get(0);
    assertThat(shippingRecord.getKey(), is(orderId));
    assertThat(shippingRecord.getValue().getOrderId(), is(orderId));

    Object stateStoreElement = testDriver.getKeyValueStore("SHIPPING_STATE_STORE").get(orderId);
    assertThat(stateStoreElement, is(nullValue()));
  }

  @Test
  void order_shipped_after_manufactured_out_of_order_events() {
    ProductDto product1 = new ProductDto(1L, "Product 1");
    ProductDto product2 = new ProductDto(2L, "Product 2");
    OrderDto orderDto = new OrderDto(Set.of(product1, product2));

    String orderId = "bc35d5f1-c3c1-4d4d-9a04-89f6e76f29cd";

    ProductManufacturedDto product1Manufactured = new ProductManufacturedDto(orderId, 1L, LocalDateTime.now());
    ProductManufacturedDto product2Manufactured = new ProductManufacturedDto(orderId, 2L, LocalDateTime.now());
    productManufacturedTopic.pipeInput(orderId, product1Manufactured);
    productManufacturedTopic.pipeInput(orderId, product2Manufactured);

    ordersTopic.pipeInput(orderId, orderDto);

    List<TestRecord<String, ShippingDto>> outputRecords = shippingTopic.readRecordsToList();
    assertThat(outputRecords.size(), is(1));

    TestRecord<String, ShippingDto> shippingRecord = outputRecords.get(0);
    assertThat(shippingRecord.getKey(), is(orderId));
    assertThat(shippingRecord.getValue().getOrderId(), is(orderId));

    Object stateStoreElement = testDriver.getKeyValueStore("SHIPPING_STATE_STORE").get(orderId);
    assertThat(stateStoreElement, is(nullValue()));
  }
}