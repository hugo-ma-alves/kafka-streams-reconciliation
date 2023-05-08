package com.hugomalves.manufacturer;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.StringJoiner;

public class ProductManufacturedDto {

  private String orderId;

  private Long productId;

  private LocalDateTime manufacturedDate;

  public ProductManufacturedDto() {
  }

  public ProductManufacturedDto(String orderId, Long productId, LocalDateTime manufacturedDate) {
    this.orderId = orderId;
    this.productId = productId;
    this.manufacturedDate = manufacturedDate;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public LocalDateTime getManufacturedDate() {
    return manufacturedDate;
  }

  public void setManufacturedDate(LocalDateTime manufacturedDate) {
    this.manufacturedDate = manufacturedDate;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProductManufacturedDto that = (ProductManufacturedDto) o;
    return productId.equals(that.productId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ProductManufacturedDto.class.getSimpleName() + "[", "]")
            .add("orderId='" + orderId + "'")
            .add("productId=" + productId)
            .add("manufacturedDate=" + manufacturedDate)
            .toString();
  }
}
