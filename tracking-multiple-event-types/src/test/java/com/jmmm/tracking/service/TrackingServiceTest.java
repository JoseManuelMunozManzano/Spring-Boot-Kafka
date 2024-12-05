package com.jmmm.tracking.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.kafka.core.KafkaTemplate;

import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.dispatch.message.TrackingStatusUpdated;
import com.jmmm.tracking.util.TestEventData;

public class TrackingServiceTest {

  private TrackingService service;
  private KafkaTemplate kafkaProducerMock;

  @BeforeEach
  void setUp() {
    kafkaProducerMock = mock(KafkaTemplate.class);
    service = new TrackingService(kafkaProducerMock);
  }

  @Test
  void process_Success() throws Exception {

        when(kafkaProducerMock.send(anyString(), any(TrackingStatusUpdated.class))).thenReturn(mock(CompletableFuture.class));

        DispatchPreparing testEvent = TestEventData.builDispatchPreparingEvent(randomUUID());
        service.process(testEvent);
        verify(kafkaProducerMock, times(1)).send(eq("tracking.status"), any(TrackingStatusUpdated.class));
  }

    @Test
    void process_ProducerThrowsException() throws Exception {
        DispatchPreparing testEvent = TestEventData.builDispatchPreparingEvent(randomUUID());
        doThrow(new RuntimeException("Producer failure")).when(kafkaProducerMock).send(eq("tracking.status"), any(TrackingStatusUpdated.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("tracking.status"), ArgumentMatchers.any(TrackingStatusUpdated.class));

        assertThat(exception.getMessage(), equalTo("Producer failure"));
    }
}
