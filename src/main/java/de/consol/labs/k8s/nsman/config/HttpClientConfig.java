package de.consol.labs.k8s.nsman.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig {
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public WebClient webClient() {
    return WebClient.builder().build();
  }
}
