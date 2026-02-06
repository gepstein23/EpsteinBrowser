package com.epstein.ingestion.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void startsInClosedState() {
        CircuitBreaker cb = new CircuitBreaker(5, 1000);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allowRequest());
    }

    @Test
    void opensAfterFailureThreshold() {
        CircuitBreaker cb = new CircuitBreaker(3, 1000);

        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allowRequest());

        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.allowRequest());
    }

    @Test
    void closesOnSuccess() {
        CircuitBreaker cb = new CircuitBreaker(2, 1000);

        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Simulate cooldown elapsed by using short cooldown
        CircuitBreaker shortCb = new CircuitBreaker(2, 1);
        shortCb.recordFailure();
        shortCb.recordFailure();

        // Wait for cooldown
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        // Should transition to HALF_OPEN
        assertTrue(shortCb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, shortCb.getState());

        shortCb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, shortCb.getState());
    }

    @Test
    void successResetsFailureCount() {
        CircuitBreaker cb = new CircuitBreaker(5, 1000);

        cb.recordFailure();
        cb.recordFailure();
        assertEquals(2, cb.getConsecutiveFailures());

        cb.recordSuccess();
        assertEquals(0, cb.getConsecutiveFailures());
    }

    @Test
    void halfOpenAfterCooldown() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 10);

        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        Thread.sleep(20);
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
        assertTrue(cb.allowRequest());
    }
}
