package com.digital.biblioteca.config.supabase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.supabase")
public class SupabaseProperties {

    private String url;

    private String key;

}
