package com.hugomalves.shipping.processor;

import com.hugomalves.order.OrderDto;
import com.hugomalves.shipping.OrderManufacturingStatus;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderProcessor extends BaseProcessor<OrderDto> {

  private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

  @Override
  protected void updateStateStoreElement(OrderManufacturingStatus manufacturingStatus, Record<String, OrderDto> record) {
    manufacturingStatus.setOrderedProducts(record.value().getItems());
  }
}
