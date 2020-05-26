package de.consol.labs.k8s.nsman.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
public class K8sNamespaceManagerConfig {
  @Bean
  K8sNamespaceManagerProperties k8sNamespaceManagerProperties() {
    return new K8sNamespaceManagerProperties();
  }

  @ConfigurationProperties(prefix = "consol.k8s.nsman")
  @Setter
  @Getter
  public static class K8sNamespaceManagerProperties {
    @NotNull
    private K8sClient k8sClient;
    @NotNull
    private K8sInformer k8sInformer;
    @DurationUnit(ChronoUnit.SECONDS)
    @NotNull
    private Duration namespaceMonitoringIntervalSec;
  }

  @Setter
  @Getter
  public static class K8sClient {
    private boolean useAutoconfig;
    @NotBlank
    private String masterUrl;
    @NotBlank
    private String caCertFile;
    @NotBlank
    private String clientCertFile;
    @NotBlank
    private String clientKeyFile;
    @NotBlank
    private String clientKeyPassphrase;
    @NotBlank
    private String clientKeyAlgo;
  }

  @Setter
  @Getter
  public static class K8sInformer {
    @DurationUnit(ChronoUnit.SECONDS)
    @NotNull
    private Duration recyncPeriodSec;
  }
}
