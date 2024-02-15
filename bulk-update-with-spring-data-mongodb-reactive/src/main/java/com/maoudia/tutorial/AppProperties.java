package com.maoudia.tutorial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration properties for the application.
 * These properties are bound from the application.yml (or application.properties) file using the specified prefix.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        /*
         * The bulk size configuration.
         * The Default value is 128
         */
        @DefaultValue("128")
        @Positive
        int bulkSize,

        /*
         * The maximum buffer size configuration.
         * The Default value is 1024.
         */
        @DefaultValue("1024")
        @Positive
        int bufferMaxSize,

        /*
         * The name of the collection.
         */
        @NotBlank
        String collectionName,

        /*
         * The key used for enriching.
         */
        @NotBlank
        String enrichingKey,

        /*
         * The URI used for enriching.
         */
        @NotNull
        URI enrichingUri,

        /*
         * Maximum number of retry attempts.
         * The Default value is 6.
         */
        @DefaultValue("6")
        int retryMaxAttempts,

        /*
         * Minimum backoff duration between retry attempts.
         * The Default value is 2 seconds.
         */
        @DefaultValue("2s")
        Duration retryMinBackOff
) {
}
