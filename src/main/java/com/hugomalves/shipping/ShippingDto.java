package com.hugomalves.shipping;

public class ShippingDto {

  private String orderId;

  public ShippingDto() {
  }

  public ShippingDto(String orderId) {
    this.orderId = orderId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }
}
