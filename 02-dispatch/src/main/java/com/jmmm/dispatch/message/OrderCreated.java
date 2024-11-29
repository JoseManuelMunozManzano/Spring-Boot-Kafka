package com.jmmm.dispatch.message;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Spring Kafka deserializará por nosotros el event a este tipo.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreated {

  UUID orderId;

  String item;
}
