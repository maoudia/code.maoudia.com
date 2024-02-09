package com.maoudia.tutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication(exclude = MongoReactiveRepositoriesAutoConfiguration.class)
@ConfigurationPropertiesScan("com.maoudia.tutorial")
public class Application implements CommandLineRunner, ExitCodeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private final AppProperties properties;
    private final CollectionService service;
    private int exitCode = 255;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
    }

    public Application(AppProperties properties, CollectionService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(final String... args) {
        service.enrichAll(properties.collectionName(), properties.enrichingKey(), properties.enrichingUri())
                .doOnSubscribe(unused -> LOGGER.info("------------------< Staring Collection Enriching Command >-------------------"))
                .doOnNext(bulkWriteResult -> LOGGER.info("Bulk write result with {} modified document(s)", bulkWriteResult.getModifiedCount()))
                .doOnError(throwable -> {
                    exitCode = 1;
                    LOGGER.error("Collection enriching failed due to : {}", throwable.getMessage(), throwable);
                })
                .doOnComplete(() -> exitCode = 0)
                .doOnTerminate(() -> LOGGER.info("------------------< Collection Enriching Command Finished >------------------"))
                .blockLast();
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

}
