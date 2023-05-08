package com.hugomalves.order;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

public class OrderDto {

  private String id;

  private LocalDateTime orderDate;

  private Set<ProductDto> items;

  public OrderDto() {
    this.id = UUID.randomUUID().toString();
    this.items = new HashSet<>();
    this.orderDate = LocalDateTime.now();
  }

  public void addItem(ProductDto productDto) {
    items.add(productDto);
  }

  public OrderDto(Set<ProductDto> items) {
    this.items = items;
  }

  public Set<ProductDto> getItems() {
    return items;
  }

  public void setItems(Set<ProductDto> items) {
    this.items = items;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDateTime orderDate) {
    this.orderDate = orderDate;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", OrderDto.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("orderDate=" + orderDate)
            .add("items=" + items)
            .toString();
  }
}
