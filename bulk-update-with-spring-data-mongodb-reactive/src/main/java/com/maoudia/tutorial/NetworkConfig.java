package com.maoudia.tutorial;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NetworkConfig {

    @Bean
    public WebClient productApiClient() {
        return WebClient.create();
    }

}
