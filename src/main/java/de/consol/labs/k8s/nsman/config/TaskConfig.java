package de.consol.labs.k8s.nsman.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import de.consol.labs.k8s.nsman.task.NamespaceManagerIdentifier;

@Configuration
public class TaskConfig {
  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    final ThreadPoolTaskScheduler threadPoolTaskScheduler =
        new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(10);
    threadPoolTaskScheduler.setThreadNamePrefix("nsman-");
    threadPoolTaskScheduler.initialize();
    return threadPoolTaskScheduler;
  }

  @Bean
  public BlockingQueue<String> namespaceManagersQueue() {
    return new LinkedBlockingQueue<>();
  }

  @Bean
  public ConcurrentMap<NamespaceManagerIdentifier, ScheduledFuture<?>> tasks() {
    return new ConcurrentHashMap<>();
  }
}
