package de.consol.labs.k8s.nsman.task;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ScheduledTaskInfo {
  private final ScheduledFuture<?> task;
  private final Instant scheduledAt;
}
