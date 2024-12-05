package com.jmmm.tracking.util;

import java.util.UUID;

import com.jmmm.dispatch.message.DispatchPreparing;

public class TestEventData {

  public static DispatchPreparing builDispatchPreparingEvent(UUID orderId) {

    return DispatchPreparing.builder()
      .orderId(orderId)
      .build();
  }

}
