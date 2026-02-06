package com.epstein.ingestion.http;

import java.util.concurrent.ThreadLocalRandom;

public class RetryPolicy {

    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final int maxRetries;

    public RetryPolicy(long initialBackoffMs, long maxBackoffMs, int maxRetries) {
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
        this.maxRetries = maxRetries;
    }

    public long calculateDelayMs(int attempt) {
        long exponentialDelay = initialBackoffMs * (1L << Math.min(attempt, 20));
        long capped = Math.min(exponentialDelay, maxBackoffMs);
        // Add jitter: random between 50% and 100% of the capped delay
        long jitter = capped / 2 + ThreadLocalRandom.current().nextLong(capped / 2 + 1);
        return jitter;
    }

    public boolean shouldRetry(int attempt) {
        return attempt < maxRetries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
