package com.maoudia.tutorial;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.internal.bulk.WriteRequest;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;

@SpringBootTest
@Testcontainers
class CollectionServiceTest {

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.2")
            .withReuse(true);

    @DynamicPropertySource
    private static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private AppProperties properties;
    @Autowired
    private CollectionService command;
    @Autowired
    private ReactiveMongoTemplate template;

    @Test
    void multipleBulkWriteResultsAreReturned() {
        Document givenDocument1 = new Document();
        givenDocument1.put("_id", "628ea3edb5110304e5e814f6");
        givenDocument1.put("type", "municipality");
        Document givenDocument2 = new Document();
        givenDocument2.put("_id", "628ea3edb5110304e5e814f7");
        givenDocument2.put("type", "street");
        Document givenDocument3 = new Document();
        givenDocument3.put("_id", "628ea3edb5110304e5e814f8");
        givenDocument3.put("type", "housenumber");
        template.insert(Arrays.asList(givenDocument1, givenDocument2, givenDocument3), properties.getCollectionName()).blockLast();

        BulkWriteResult expectedBulkWriteResult1 = BulkWriteResult.acknowledged(WriteRequest.Type.REPLACE, 2, 2, Collections.emptyList(),
                Collections.emptyList());
        BulkWriteResult expectedBulkWriteResult2 = BulkWriteResult.acknowledged(WriteRequest.Type.REPLACE, 1, 1, Collections.emptyList(),
                Collections.emptyList());

        command.enrichAll( properties.getCollectionName(), properties.getEnrichingKey() , properties.getEnrichingUri())
                .as(StepVerifier::create)
                .expectNext(expectedBulkWriteResult1)
                .expectNext(expectedBulkWriteResult2)
                .verifyComplete();
    }
}
