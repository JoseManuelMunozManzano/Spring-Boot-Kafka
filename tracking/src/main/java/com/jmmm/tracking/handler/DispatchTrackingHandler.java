package com.jmmm.tracking.handler;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.tracking.service.TrackingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class DispatchTrackingHandler {

  private final TrackingService trackingService;

  @KafkaListener(id = "dispatchTrackingConsumerClient",
                 topics = "dispatch.tracking",
                 groupId = "tracking.dispatch.tracking",
                 containerFactory = "kafkaListenerContainerFactory"
  )
  public void listen(DispatchPreparing payload) throws Exception {
    log.info("Received message: payload: " + payload);

    try {
      trackingService.process(payload);
    } catch (Exception e) {
      log.error("Processing failure", e);
    }
  }
}
