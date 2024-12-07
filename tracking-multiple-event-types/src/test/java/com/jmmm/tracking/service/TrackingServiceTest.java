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

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.jmmm.dispatch.message.DispatchCompleted;
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
  void process_DispatchPreparingSuccess() throws Exception {
        when(kafkaProducerMock.send(anyString(), any(TrackingStatusUpdated.class))).thenReturn(mock(CompletableFuture.class));

        DispatchPreparing testEvent = TestEventData.builDispatchPreparingEvent(randomUUID());
        service.processDispatchPreparing(testEvent);
        verify(kafkaProducerMock, times(1)).send(eq("tracking.status"), any(TrackingStatusUpdated.class));
  }

    @Test
    void process_DispatchPreparingProducerThrowsException() {
        doThrow(new RuntimeException("dispatch preparing producer failure")).when(kafkaProducerMock).send(eq("tracking.status"), any(TrackingStatusUpdated.class));

        DispatchPreparing testEvent = TestEventData.builDispatchPreparingEvent(randomUUID());
        Exception exception = assertThrows(RuntimeException.class, () -> service.processDispatchPreparing(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("tracking.status"), ArgumentMatchers.any(TrackingStatusUpdated.class));
        assertThat(exception.getMessage(), equalTo("dispatch preparing producer failure"));
    }

    @Test
    void process_DispatchedSuccess() throws Exception {
        when(kafkaProducerMock.send(anyString(), any(TrackingStatusUpdated.class))).thenReturn(mock(CompletableFuture.class));

        DispatchCompleted testEvent = TestEventData.buildDispatchCompletedEvent(randomUUID(), LocalDate.now().toString());
        service.processDispatched(testEvent);
        verify(kafkaProducerMock, times(1)).send(eq("tracking.status"), any(TrackingStatusUpdated.class));
    }

    @Test
    void process_DispatchedProducerThrowsException() {
      doThrow(new RuntimeException("dispatched producer failure")).when(kafkaProducerMock).send(eq("tracking.status"), any(TrackingStatusUpdated.class));

      DispatchCompleted testEvent = TestEventData.buildDispatchCompletedEvent(randomUUID(), LocalDate.now().toString());
      Exception exception = assertThrows(RuntimeException.class, () -> service.processDispatched(testEvent));

      verify(kafkaProducerMock, times(1)).send(eq("tracking.status"), any(TrackingStatusUpdated.class));
      assertThat(exception.getMessage(), equalTo("dispatched producer failure"));
    }
}
