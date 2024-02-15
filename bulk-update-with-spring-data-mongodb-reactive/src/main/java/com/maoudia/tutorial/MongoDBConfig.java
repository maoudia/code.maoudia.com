package com.maoudia.tutorial;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class MongoDBConfig {

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveMongoTransactionManager mongoTransactionManager) {
        return TransactionalOperator.create(mongoTransactionManager);
    }

    @Bean
    public ReactiveMongoTransactionManager mongoTransactionManager(ReactiveMongoDatabaseFactory mongoDatabaseFactory) {
        return new ReactiveMongoTransactionManager(mongoDatabaseFactory);
    }

}
