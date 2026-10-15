package com.digital.smartteca.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SearchClientConfig {

    @Value("${services.search-url}")
    private String searchUrl;

    @Bean
    public RestClient searchServiceRestClient() {
        return RestClient.builder()
                .baseUrl(searchUrl)
                .build();
    }

}
