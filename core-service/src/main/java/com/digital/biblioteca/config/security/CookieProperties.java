package com.digital.biblioteca.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.security.cookie")
public class CookieProperties {

    private boolean secure = false;
    private String sameSite = "Lax";
    private String domain = "";

}
