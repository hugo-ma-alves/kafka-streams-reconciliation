package com.hugomalves.shipping.processor;

import com.hugomalves.manufacturer.ProductManufacturedDto;
import com.hugomalves.shipping.OrderManufacturingStatus;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManufacturerProcessor extends BaseProcessor<ProductManufacturedDto> {

  private static final Logger log = LoggerFactory.getLogger(ManufacturerProcessor.class);

  @Override
  protected void updateStateStoreElement(OrderManufacturingStatus manufacturingStatus, Record<String, ProductManufacturedDto> record) {
    manufacturingStatus.setProductManufactured(record.value());
  }
}
