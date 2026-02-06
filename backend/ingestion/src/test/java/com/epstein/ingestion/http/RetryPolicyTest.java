package com.epstein.ingestion.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void shouldRetryBelowMax() {
        RetryPolicy policy = new RetryPolicy(1000, 60000, 3);
        assertTrue(policy.shouldRetry(0));
        assertTrue(policy.shouldRetry(1));
        assertTrue(policy.shouldRetry(2));
        assertFalse(policy.shouldRetry(3));
    }

    @Test
    void calculateDelayIncreases() {
        RetryPolicy policy = new RetryPolicy(1000, 120000, 5);

        // The delay should generally increase, but due to jitter we test ranges
        long delay0 = policy.calculateDelayMs(0);
        assertTrue(delay0 >= 500 && delay0 <= 1000, "Delay at attempt 0 should be between 500-1000ms, was " + delay0);

        long delay1 = policy.calculateDelayMs(1);
        assertTrue(delay1 >= 1000 && delay1 <= 2000, "Delay at attempt 1 should be between 1000-2000ms, was " + delay1);
    }

    @Test
    void calculateDelayCapsAtMax() {
        RetryPolicy policy = new RetryPolicy(1000, 5000, 10);

        long delay = policy.calculateDelayMs(20);
        assertTrue(delay <= 5000, "Delay should not exceed max 5000ms, was " + delay);
    }

    @Test
    void maxRetriesReturnedCorrectly() {
        RetryPolicy policy = new RetryPolicy(1000, 60000, 7);
        assertEquals(7, policy.getMaxRetries());
    }
}
