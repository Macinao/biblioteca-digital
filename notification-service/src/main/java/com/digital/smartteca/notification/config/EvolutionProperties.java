package com.digital.smartteca.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.evolution")
public class EvolutionProperties {

    private String url;
    private String apikey;
    private String instance;

}
