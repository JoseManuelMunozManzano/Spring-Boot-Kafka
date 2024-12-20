package com.jmmm.dispatch.handler;

import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.service.DispatchService;
import com.jmmm.dispatch.util.TestEventData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OrderCreatedHandlerTest {

    // Solo testeamos el handler, el service va a ser un mock.
    private OrderCreatedHandler handler;
    private DispatchService dispatchServiceMock;

    @BeforeEach
    void setUp() {
        dispatchServiceMock = mock(DispatchService.class);
        handler = new OrderCreatedHandler(dispatchServiceMock);
    }

    @Test
    void listen_Success() throws Exception {
        OrderCreated testEvent = TestEventData.builOrderCreatedEvent(randomUUID(), randomUUID().toString());
        handler.listen(testEvent);
        verify(dispatchServiceMock, times(1)).process(testEvent);
    }

    // Como puede haber excepciones, hacemos testing para ver si estos flujos se comportan como esperamos.
    @Test
    void listen_ServiceThrowsException() throws Exception {
        OrderCreated testEvent = TestEventData.builOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RuntimeException("Service failure")).when(dispatchServiceMock).process(testEvent);

        handler.listen(testEvent);
        
        verify(dispatchServiceMock, times(1)).process(testEvent);
    }
}
