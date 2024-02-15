package com.maoudia.tutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configuration
public class MongoDBConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBConfig.class);

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveMongoTransactionManager mongoTransactionManager) {
        return TransactionalOperator.create(mongoTransactionManager);
    }

    @Bean
    public ReactiveMongoTransactionManager mongoTransactionManager(ReactiveMongoDatabaseFactory mongoDatabaseFactory) {
        return new ReactiveMongoTransactionManager(mongoDatabaseFactory);
    }

    @Bean
    public RetryBackoffSpec retryBackoffSpec(AppProperties appProperties) {

        return Retry.backoff(appProperties.retryMaxAttempts(), appProperties.retryMinBackOff())
                .scheduler(Schedulers.boundedElastic())
                .doAfterRetry(retrySignal -> LOGGER.warn("Retry attempt {}/{} failed",
                        retrySignal.totalRetries() + 1,
                        appProperties.retryMaxAttempts()))
                .onRetryExhaustedThrow((_retryBackoffSpec, retrySignal) -> {
                    throw new AppException(STR.
                            """
                            Failed to retrieve entry after \{retrySignal.totalRetries()} retries.
                            The operation may be experiencing issues or the service is unavailable.
                            """
                    );
                });
    }

}
