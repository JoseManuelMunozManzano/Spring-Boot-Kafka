package com.jmmm.dispatch.service;

import org.springframework.stereotype.Service;

import com.jmmm.dispatch.message.OrderCreated;

// Consumer
// 2. Servicio que solo procesa el evento consumido.
@Service
public class DispatchService {

    public void process(OrderCreated payload) {
        // todo
    }
}
