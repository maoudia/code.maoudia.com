package com.maoudia.tutorial;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import static java.lang.StringTemplate.STR;

@Configuration
public class NetworkConfig {

    @Bean
    public WebClient productApiClient() {
        return WebClient.create();
    }

}
