package com.maoudia.tutorial;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.net.URI;
import java.util.Date;
import java.util.List;

@Service
public class CollectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionService.class);
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
    private final MeterRegistry meterRegistry;

    private final ObservationRegistry observationRegistry;

    public CollectionService(AppProperties properties,
                             ReactiveMongoTemplate template,
                             WebClient client,
                             TransactionalOperator transactionalOperator,
                             MeterRegistry meterRegistry,
                             ObservationRegistry observationRegistry) {
        this.properties = properties;
        this.template = template;
        this.client = client;
        this.transactionalOperator = transactionalOperator;
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
    }

    public Flux<BulkWriteResult> enrichAll(String collectionName,
                                           String enrichingKey,
                                           URI enrichingUri) {
        return template.findAll(Document.class, collectionName)
                .name("app.documents.flux")
                .tag("source", "mongodb")
                .tap(Micrometer.metrics(meterRegistry))
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
                .bodyToMono(Document.class)
                .publishOn(Schedulers.boundedElastic())
                .tap(Micrometer.observation(
                        observationRegistry))
                .retryWhen(getRetryBackoffSpec())
                .name("app.enriching.call")
                .tag("source", "http")
                .tap(Micrometer.metrics(meterRegistry));
    }

    private Publisher<BulkWriteResult> bulkWrite(Flux<ReplaceOneModel<Document>> updateOneModelFlux,
                                                 String collectionName) {
        return updateOneModelFlux
                .name("app.documents.bulk")
                .tap(Micrometer.metrics(meterRegistry))
                .collectList()
                .zipWith(template.getCollection(collectionName))
                .flatMapMany(tuple -> tuple.getT2().bulkWrite(tuple.getT1(), BULK_WRITE_OPTIONS))
                .as(transactionalOperator::transactional)
                .name("app.transactions")
                .tap(Micrometer.observation(
                        observationRegistry,
                        registry -> Observation.createNotStarted(
                                "transaction",
                                () -> {
                                    Observation.Context context = new Observation.Context();
                                    context.addLowCardinalityKeyValues(KeyValues.of("context", "transaction"));
                                    return context;
                                },
                                registry))
                );
    }

    private RetryBackoffSpec getRetryBackoffSpec() {
        return Retry.backoff(properties.retryMaxAttempts(), properties.retryMinBackOff())
                .scheduler(Schedulers.boundedElastic())
                .doAfterRetry(retrySignal -> LOGGER.warn("Retry attempt {}/{} failed",
                        retrySignal.totalRetries() + 1,
                        properties.retryMaxAttempts()))
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
