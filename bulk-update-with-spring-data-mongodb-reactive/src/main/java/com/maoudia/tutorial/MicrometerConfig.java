package com.maoudia.tutorial;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationTextPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerConfig.class);

    @Bean
    public ObservationTextPublisher transactionObservationTextPublisher() {
        return new ObservationTextPublisher(
                LOGGER::info,
                context -> context
                        .getLowCardinalityKeyValues()
                        .stream()
                        .anyMatch(keyValue -> keyValue.getKey().equals("context") && keyValue.getValue().equals("transaction")),
                Observation.Context::getName);
    }

}
