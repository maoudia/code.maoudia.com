package com.maoudia.tutorial;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties{
    private int bulkSize;
    private int bufferMaxSize;
    private String collectionName;
    private String enrichingKey;
    private String enrichingUri;

    public int getBulkSize() {
        return bulkSize;
    }

    public void setBulkSize(final int bulkSize) {
        this.bulkSize = bulkSize;
    }

    public int getBufferMaxSize() {
        return bufferMaxSize;
    }

    public void setBufferMaxSize(final int bufferMaxSize) {
        this.bufferMaxSize = bufferMaxSize;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(final String collectionName) {
        this.collectionName = collectionName;
    }

    public String getEnrichingKey() {
        return enrichingKey;
    }

    public void setEnrichingKey(final String enrichingKey) {
        this.enrichingKey = enrichingKey;
    }

    public String getEnrichingUri() {
        return enrichingUri;
    }

    public void setEnrichingUri(final String enrichingUri) {
        this.enrichingUri = enrichingUri;
    }
}