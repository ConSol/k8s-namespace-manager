package de.consol.labs.k8s.nsman.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import de.consol.labs.k8s.nsman.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskManager implements Runnable {
  private static final Duration EPSILON = Duration.ofSeconds(5);

  private final ConcurrentMap<NamespaceManagerIdentifier, ScheduledTaskInfo> tasks =
      new ConcurrentHashMap<>();

  private final BlockingQueue<String> namespaceManagersQueue;
  private final ObjectFactory<CheckNamespaceManagerTask> taskFactory;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final SchedulingUtil schedulingUtil;

  @Override
  public void run() {
    log.info("processing has been started");
    while (true) {
      if (namespaceManagersQueue.isEmpty()) {
        log.info("queue is empty");
      }
      final String key;
      try {
        key = namespaceManagersQueue.take();
      } catch (final InterruptedException e) {
        ThreadUtil.logAndInterrupt(e);
        return;
      }
      final NamespaceManagerIdentifier id = getManagerId(key);
      if (Objects.isNull(id)) {
        continue;
      }
      log.info("start processing. manager id = {}", id);
      try {
        scheduleCheckTask(id);
      } catch (final RuntimeException e) {
        log.error(
            "failure during task scheduling but show must go on: queue processing will be continued",
            e);
        continue;
      }
      log.info("finished processing. manager id = {}", id);
    }
  }

  private NamespaceManagerIdentifier getManagerId(final String key) {
    log.debug("key = {}", key);
    if (Objects.isNull(key) || key.isBlank()) {
      log.error("ignoring empty key");
      return null;
    }
    final String[] parts = key.split("/");
    if (parts.length != 2) {
      log.error("ignoring malformed key {}", key);
      return null;
    }
    return new NamespaceManagerIdentifier(parts[0], parts[1]);
  }

  private void scheduleCheckTask(final NamespaceManagerIdentifier id) {
    final Instant scheduleAt = schedulingUtil.getScheduleAt(id);
    if (Objects.isNull(scheduleAt)) {
      log.error("could not determine schedule time");
      return;
    }
    log.debug("scheduleAt = {}", scheduleAt);
    final ScheduledTaskInfo t = tasks.get(id);
    if (Objects.isNull(t) || t.getTask().isDone()) {
      scheduleNewTask(id, scheduleAt);
      return;
    }
    final Duration diff =
        Duration.between(scheduleAt, t.getScheduledAt()).abs();
    log.debug("diff = {} sec", diff.toSeconds());
    if (diff.compareTo(EPSILON) <= 0
        || scheduleAt.compareTo(t.getScheduledAt()) >= 0) {
      log.debug("there is already a task scheduled at {}", t.getScheduledAt());
      return;
    }
    t.getTask().cancel(false);
    log.debug("cancelled task scheduled at {}", t.getScheduledAt());
    scheduleNewTask(id, scheduleAt);
  }

  private void scheduleNewTask(final NamespaceManagerIdentifier id,
      final Instant scheduleAt) {
    final CheckNamespaceManagerTask newTask = taskFactory.getObject();
    newTask.setNamespaceManagerIdentifier(id);
    final ScheduledFuture<?> newScheduledTask =
        taskScheduler.schedule(newTask, scheduleAt);
    tasks.put(id, new ScheduledTaskInfo(newScheduledTask, scheduleAt));
    log.info("scheduled new task at {}", scheduleAt);
  }
}
