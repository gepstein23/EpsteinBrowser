package com.epstein.ingestion.http;

import com.epstein.ingestion.config.IngestionProperties;
import com.epstein.ingestion.metrics.IngestionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitedHttpClientTest {

    private HttpClient mockHttpClient;
    private IngestionProperties properties;
    private IngestionMetrics metrics;
    private RateLimitedHttpClient rateLimitedClient;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        mockHttpClient = mock(HttpClient.class);
        properties = new IngestionProperties();
        properties.getRateLimit().setRequestsPerSecond(10);
        properties.getRateLimit().setPoliteDelayMs(0);
        metrics = new IngestionMetrics(new SimpleMeterRegistry());
        rateLimitedClient = new RateLimitedHttpClient(mockHttpClient, properties, metrics);
    }

    @Test
    @SuppressWarnings("unchecked")
    void successfulGetBytesReturnsContent() throws Exception {
        HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("test content".getBytes());
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        HttpResponse<byte[]> response = rateLimitedClient.getBytes("https://example.com/test.pdf");
        assertEquals(200, response.statusCode());
        assertArrayEquals("test content".getBytes(), response.body());
    }

    @Test
    @SuppressWarnings("unchecked")
    void successfulGetStringReturnsContent() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("<html>test</html>");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        HttpResponse<String> response = rateLimitedClient.getString("https://example.com/page");
        assertEquals(200, response.statusCode());
        assertEquals("<html>test</html>", response.body());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retriesOn429ThenSucceeds() throws Exception {
        HttpResponse<byte[]> retryResponse = mock(HttpResponse.class);
        when(retryResponse.statusCode()).thenReturn(429);

        HttpResponse<byte[]> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        when(successResponse.body()).thenReturn("data".getBytes());

        // Use short backoff for testing
        properties.getRateLimit().setBackoffInitialMs(1);
        properties.getRateLimit().setBackoffMaxMs(10);
        rateLimitedClient = new RateLimitedHttpClient(mockHttpClient, properties, metrics);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(retryResponse)
                .thenReturn(successResponse);

        HttpResponse<byte[]> response = rateLimitedClient.getBytes("https://example.com/test.pdf");
        assertEquals(200, response.statusCode());
        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void throwsOnPersistent4xx() throws Exception {
        HttpResponse<byte[]> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(404);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(errorResponse);

        assertThrows(IOException.class, () ->
                rateLimitedClient.getBytes("https://example.com/notfound.pdf"));
    }

    @Test
    void circuitBreakerIsAccessible() {
        CircuitBreaker cb = rateLimitedClient.getCircuitBreaker();
        assertNotNull(cb);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }
}
