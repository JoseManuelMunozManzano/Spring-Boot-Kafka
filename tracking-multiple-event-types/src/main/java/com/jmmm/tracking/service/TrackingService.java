package com.jmmm.tracking.service;

import com.jmmm.dispatch.message.DispatchCompleted;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.dispatch.message.TrackingStatusUpdated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

  private static final String TRACKING_STATUS_TOPIC = "tracking.status";

  private final KafkaTemplate<String, Object> kafkaProducer;

  public void processDispatchPreparing(DispatchPreparing dispatchPreparing) throws Exception {

    log.info("Received dispatch preparing message: " + dispatchPreparing);

    TrackingStatusUpdated trackingStatusUpdated = TrackingStatusUpdated.builder()
                          .orderId(dispatchPreparing.getOrderId())
                          .status(TrackingStatus.PREPARING)
                          .build();

    kafkaProducer.send(TRACKING_STATUS_TOPIC, trackingStatusUpdated).get();
  }

  public void processDispatched(DispatchCompleted dispatchCompleted) throws Exception {

    log.info("Received dispatched message: " + dispatchCompleted);

    TrackingStatusUpdated trackingStatusUpdated = TrackingStatusUpdated.builder()
            .orderId(dispatchCompleted.getOrderId())
            .status(TrackingStatus.DISPATCHED)
            .build();

    kafkaProducer.send(TRACKING_STATUS_TOPIC, trackingStatusUpdated).get();
  }
}
