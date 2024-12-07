package com.jmmm.tracking.util;

import java.util.UUID;

import com.jmmm.dispatch.message.DispatchCompleted;
import com.jmmm.dispatch.message.DispatchPreparing;

public class TestEventData {

  public static DispatchPreparing builDispatchPreparingEvent(UUID orderId) {

    return DispatchPreparing.builder()
      .orderId(orderId)
      .build();
  }

  public static DispatchCompleted buildDispatchCompletedEvent(UUID orderId, String date) {

    return DispatchCompleted.builder()
            .orderId(orderId)
            .date(date)
            .build();
  }

}
