package com.jmmm.dispatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jmmm.dispatch.message.OrderCreated;
import com.jmmm.dispatch.util.TestEventData;

import static java.util.UUID.randomUUID;

class DispatchServiceTest {

    private DispatchService service;

    @BeforeEach
    void setUp() {
        service = new DispatchService();
    }

    @Test
    void process() {
        OrderCreated testEvent = TestEventData.builOrderCreatedEvent(randomUUID(), randomUUID().toString());
        service.process(testEvent);
    }
}
