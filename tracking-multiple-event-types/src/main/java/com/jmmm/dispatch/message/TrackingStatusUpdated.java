package com.jmmm.dispatch.message;

import java.util.UUID;

import com.jmmm.tracking.service.TrackingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingStatusUpdated {

  UUID orderId;
  TrackingStatus status;
}
