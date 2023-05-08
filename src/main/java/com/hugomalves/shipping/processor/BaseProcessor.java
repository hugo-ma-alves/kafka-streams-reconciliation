package com.hugomalves.shipping.processor;

import com.hugomalves.shipping.OrderManufacturingStatus;
import com.hugomalves.shipping.ShippingDto;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

public abstract class BaseProcessor<T> implements Processor<String, T, String, ShippingDto> {

  private static final Logger log = LoggerFactory.getLogger(BaseProcessor.class);

  private static final String SHIPPING_STATE_STORE = "SHIPPING_STATE_STORE";

  private ProcessorContext<String, ShippingDto> context;

  private KeyValueStore<String, OrderManufacturingStatus> stateStore;

  @Override
  public void init(ProcessorContext<String, ShippingDto> context) {
    Processor.super.init(context);
    this.context = context;
    this.stateStore = context.getStateStore(SHIPPING_STATE_STORE);
  }

  public void process(Record<String, T> record) {
    String orderId = record.key();
    OrderManufacturingStatus manufacturingStatus = stateStore.get(orderId);

    if (manufacturingStatus == null) {
      log.info("Order {} is not yet present in state store, creating new entry", orderId);
      manufacturingStatus = new OrderManufacturingStatus(orderId);
    }

    updateStateStoreElement(manufacturingStatus, record);
    stateStore.put(orderId, manufacturingStatus);

    log.info("Order {} is present in state store, checking if all items are ready to be shipped", orderId);
    boolean isOrderComplete = manufacturingStatus.isOrderComplete();
    if (isOrderComplete) {
      log.info("All items of order {} were manufactured, forwarding event to shipping", orderId);
      Record<String, ShippingDto> shippingRecord = new Record<>(orderId, new ShippingDto(orderId), currentTimeMillis());
      stateStore.delete(orderId);
      context.forward(shippingRecord);
    } else {
      log.info("Not all items of order {} were manufactured, waiting", orderId);
    }
  }

  protected abstract void updateStateStoreElement(OrderManufacturingStatus manufacturingStatus, Record<String, T> record);
}
