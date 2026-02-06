package com.epstein.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {

    private RateLimit rateLimit = new RateLimit();
    private Concurrency concurrency = new Concurrency();
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private S3Config s3 = new S3Config();
    private Sources sources = new Sources();

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public Concurrency getConcurrency() { return concurrency; }
    public void setConcurrency(Concurrency concurrency) { this.concurrency = concurrency; }

    public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }

    public S3Config getS3() { return s3; }
    public void setS3(S3Config s3) { this.s3 = s3; }

    public Sources getSources() { return sources; }
    public void setSources(Sources sources) { this.sources = sources; }

    public static class RateLimit {
        private int requestsPerSecond = 5;
        private long politeDelayMs = 200;
        private long backoffInitialMs = 2000;
        private long backoffMaxMs = 120000;

        public int getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(int requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }

        public long getPoliteDelayMs() { return politeDelayMs; }
        public void setPoliteDelayMs(long politeDelayMs) { this.politeDelayMs = politeDelayMs; }

        public long getBackoffInitialMs() { return backoffInitialMs; }
        public void setBackoffInitialMs(long backoffInitialMs) { this.backoffInitialMs = backoffInitialMs; }

        public long getBackoffMaxMs() { return backoffMaxMs; }
        public void setBackoffMaxMs(long backoffMaxMs) { this.backoffMaxMs = backoffMaxMs; }
    }

    public static class Concurrency {
        private int maxDownloads = 3;

        public int getMaxDownloads() { return maxDownloads; }
        public void setMaxDownloads(int maxDownloads) { this.maxDownloads = maxDownloads; }
    }

    public static class CircuitBreakerConfig {
        private int failureThreshold = 10;
        private long cooldownMs = 300000; // 5 minutes

        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }

        public long getCooldownMs() { return cooldownMs; }
        public void setCooldownMs(long cooldownMs) { this.cooldownMs = cooldownMs; }
    }

    public static class S3Config {
        private String bucket = "epstein-dev-documents";
        private String rawPrefix = "raw";

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }

        public String getRawPrefix() { return rawPrefix; }
        public void setRawPrefix(String rawPrefix) { this.rawPrefix = rawPrefix; }
    }

    public static class Sources {
        private String dojDisclosuresBaseUrl = "https://www.justice.gov/epstein/doj-disclosures";
        private String courtRecordsBaseUrl = "https://www.justice.gov/epstein/court-records";
        private String foiaBaseUrl = "https://www.justice.gov/epstein/foia";

        public String getDojDisclosuresBaseUrl() { return dojDisclosuresBaseUrl; }
        public void setDojDisclosuresBaseUrl(String dojDisclosuresBaseUrl) { this.dojDisclosuresBaseUrl = dojDisclosuresBaseUrl; }

        public String getCourtRecordsBaseUrl() { return courtRecordsBaseUrl; }
        public void setCourtRecordsBaseUrl(String courtRecordsBaseUrl) { this.courtRecordsBaseUrl = courtRecordsBaseUrl; }

        public String getFoiaBaseUrl() { return foiaBaseUrl; }
        public void setFoiaBaseUrl(String foiaBaseUrl) { this.foiaBaseUrl = foiaBaseUrl; }
    }
}
