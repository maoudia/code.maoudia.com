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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;

@SpringBootTest
@Testcontainers
class CollectionServiceTest {

    @Container
    public static GenericContainer<?> jsonServerContainer = new GenericContainer<>("clue/json-server:latest")
            .withExposedPorts(80)
            .withFileSystemBind("./data/product/db.json", "/data/db.json", BindMode.READ_ONLY)
            .waitingFor(Wait.forHttp("/").forStatusCode(200).forPort(80))
            .withReuse(true);

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.2")
            .withCommand("--replSet rs0 --bind_ip_all");

    @DynamicPropertySource
    private static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("app.enriching-uri", () -> "http://" + jsonServerContainer.getHost() + ":" + jsonServerContainer.getMappedPort(80) + "/products/1");
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
        template.insert(Arrays.asList(givenDocument1, givenDocument2, givenDocument3), properties.collectionName()).blockLast();

        BulkWriteResult expectedBulkWriteResult1 = BulkWriteResult.acknowledged(WriteRequest.Type.REPLACE, 2, 2, Collections.emptyList(),
                Collections.emptyList());
        BulkWriteResult expectedBulkWriteResult2 = BulkWriteResult.acknowledged(WriteRequest.Type.REPLACE, 1, 1, Collections.emptyList(),
                Collections.emptyList());

        command.enrichAll(properties.collectionName(), properties.enrichingKey() , properties.enrichingUri())
                .as(StepVerifier::create)
                .expectNext(expectedBulkWriteResult1)
                .expectNext(expectedBulkWriteResult2)
                .verifyComplete();
    }
}
