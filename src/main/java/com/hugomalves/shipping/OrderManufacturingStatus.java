package com.hugomalves.shipping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hugomalves.manufacturer.ProductManufacturedDto;
import com.hugomalves.order.ProductDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class OrderManufacturingStatus {

  private static final Logger log = LoggerFactory.getLogger(OrderManufacturingStatus.class);

  private String orderId;

  private Set<ProductDto> orderedProducts;

  private Set<ProductManufacturedDto> manufacturedProducts;

  public OrderManufacturingStatus() {
  }

  public OrderManufacturingStatus(String orderId) {
    this.orderId = orderId;
    this.orderedProducts = new HashSet<>();
    this.manufacturedProducts = new HashSet<>();
  }

  @JsonIgnore
  public boolean isOrderComplete() {
    log.info("{}/{} manufactured products", manufacturedProducts.size(), orderedProducts.size());
    return manufacturedProducts.size() == orderedProducts.size();
  }

  public void setOrderedProducts(Set<ProductDto> orderProducts) {
    this.orderedProducts = orderProducts;
  }

  public void setProductManufactured(ProductManufacturedDto product) {
    manufacturedProducts.add(product);
  }

  public Set<ProductDto> getOrderedProducts() {
    return orderedProducts;
  }

  public Set<ProductManufacturedDto> getManufacturedProducts() {
    return manufacturedProducts;
  }
}
