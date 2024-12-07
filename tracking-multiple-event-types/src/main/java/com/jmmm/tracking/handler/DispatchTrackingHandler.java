package com.jmmm.tracking.handler;

import com.jmmm.dispatch.message.DispatchCompleted;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.tracking.service.TrackingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
@KafkaListener(id = "dispatchTrackingConsumerClient",
        topics = "dispatch.tracking",
        groupId = "tracking.dispatch.tracking",
        containerFactory = "kafkaListenerContainerFactory"
)
public class DispatchTrackingHandler {

  private final TrackingService trackingService;

  @KafkaHandler
  public void listen(DispatchPreparing payload) throws Exception {
    log.info("DispatchPreparing. Received message: payload: " + payload);

    try {
      trackingService.processDispatchPreparing(payload);
    } catch (Exception e) {
      log.error("DispatchPreparing processing failure", e);
    }
  }

  @KafkaHandler
  public void listen(DispatchCompleted payload) throws Exception {
    log.info("DispatchCompleted. Received message: payload: " + payload);

    try {
      trackingService.processDispatched(payload);
    } catch (Exception e) {
      log.error("DispatchCompleted processing failure", e);
    }
  }
}
