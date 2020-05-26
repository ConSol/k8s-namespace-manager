package de.consol.labs.k8s.nsman.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestOperations;

@Configuration
public class HttpClientConfig {
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public RestOperations restTemplate(
      final RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.build();
  }
}
