package com.jmmm.tracking.handler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jmmm.dispatch.message.DispatchPreparing;
import com.jmmm.tracking.service.TrackingService;
import com.jmmm.tracking.util.TestEventData;

import static java.util.UUID.randomUUID;

public class DispatchTrackingHandlerTest {

  // Solo testeamos el handler, el service va a ser un mock.
  private DispatchTrackingHandler handler;
  private TrackingService trackingServiceMock;

  @BeforeEach
  void setUp() {
    trackingServiceMock = mock(TrackingService.class);
    handler = new DispatchTrackingHandler(trackingServiceMock);
  }

  @Test
  void listen_Success() throws Exception {
    DispatchPreparing testEvent = TestEventData.builDispatchPreparingEvent(randomUUID());
    handler.listen(testEvent);
    verify(trackingServiceMock, times(1)).process(testEvent);
  }

  @Test
  void listen_ServiceThrowsException() throws Exception {
    DispatchPreparing testEvent = TestEventData.builDispatchPreparingEvent(randomUUID());
    doThrow(new RuntimeException("Service failure")).when(trackingServiceMock).process(testEvent);

    handler.listen(testEvent);

    verify(trackingServiceMock, times(1)).process(testEvent);
  }
}
