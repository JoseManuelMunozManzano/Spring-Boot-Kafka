package com.jmmm.dispatch.util;

import java.util.UUID;

import com.jmmm.dispatch.message.OrderCreated;

public class TestEventData {

  public static OrderCreated builOrderCreatedEvent(UUID orderId, String item) {
    return OrderCreated.builder()
        .orderId(orderId)
        .item(item)
        .build();
  }
}
