package com.maoudia.tutorial;

import com.mongodb.TransactionOptions;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Date;

@Service
public class CollectionService {
    private static final ReplaceOptions REPLACE_OPTIONS = new ReplaceOptions();
    private static final BulkWriteOptions BULK_WRITE_OPTIONS = new BulkWriteOptions().ordered(false);

    private static ReplaceOneModel<Document> toReplaceOneModel(Document document) {
        return new ReplaceOneModel<>(
                Filters.eq("_id", document.get("_id")),
                document,
                REPLACE_OPTIONS
        );
    }

    private final AppProperties properties;
    private final ReactiveMongoTemplate template;
    private final WebClient client;
    private final TransactionalOperator transactionalOperator;

    public CollectionService(AppProperties properties,
                             ReactiveMongoTemplate template,
                             WebClient client,
                             TransactionalOperator transactionalOperator) {
        this.properties = properties;
        this.template = template;
        this.client = client;
        this.transactionalOperator = transactionalOperator;
    }

    public Flux<BulkWriteResult> enrichAll(String collectionName,
                                           String enrichingKey,
                                           URI enrichingUri) {
        return template.findAll(Document.class, collectionName)
                .onBackpressureBuffer(properties.bufferMaxSize())
                .flatMap(document -> enrich(document, enrichingKey, enrichingUri))
                .map(CollectionService::toReplaceOneModel)
                .window(properties.bulkSize())
                .flatMap(replaceOneModelFlux -> bulkWrite(replaceOneModelFlux, collectionName));
    }

    private Publisher<Document> enrich(Document document,
                                       String enrichingKey,
                                       URI enrichingUri) {
        return getEnrichingDocument(enrichingUri)
                .map(enrichingDocument -> {
                    document.put(enrichingKey, enrichingDocument);
                    document.put("updatedAt", new Date());
                    return document;
                });
    }

    private Mono<Document> getEnrichingDocument(URI enrichingUri) {
        return client.get()
                .uri(enrichingUri)
                .retrieve()
                .bodyToMono(Document.class);
    }

    private Publisher<BulkWriteResult> bulkWrite(Flux<ReplaceOneModel<Document>> updateOneModelFlux,
                                            String collectionName) {
        return updateOneModelFlux
                .collectList()
                .flatMapMany(updateOneModels -> template.getCollection(collectionName)
                        .flatMapMany(collection -> collection.bulkWrite(updateOneModels, BULK_WRITE_OPTIONS)))
                .as(transactionalOperator::transactional);
    }

}
