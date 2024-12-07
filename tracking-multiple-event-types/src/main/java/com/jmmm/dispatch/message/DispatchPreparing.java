package com.jmmm.dispatch.message;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Se crea este event en este package porque si no da el error siguiente:
// The class 'com.jmmm.dispatch.message.DispatchPreparing' is not in the trusted packages
// Eso es porque en el proyecto dispatch, esta clase está en ese paquete com.jmmm.dispatch.message
//
// En un proyecto real, este package sería un proyecto aparte que se importaría en el POM.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchPreparing {

  UUID orderId;
}
