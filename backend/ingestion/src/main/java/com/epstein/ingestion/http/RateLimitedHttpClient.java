package com.epstein.ingestion.http;

import com.epstein.ingestion.config.IngestionProperties;
import com.epstein.ingestion.metrics.IngestionMetrics;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitedHttpClient {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedHttpClient.class);

    private final HttpClient httpClient;
    private final IngestionProperties properties;
    private final IngestionMetrics metrics;
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    private final Bucket globalBucket;
    private final Map<String, Bucket> hostBuckets = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestByHost = new ConcurrentHashMap<>();

    public RateLimitedHttpClient(HttpClient httpClient, IngestionProperties properties,
                                 IngestionMetrics metrics) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.metrics = metrics;

        this.circuitBreaker = new CircuitBreaker(
                properties.getCircuitBreaker().getFailureThreshold(),
                properties.getCircuitBreaker().getCooldownMs()
        );

        this.retryPolicy = new RetryPolicy(
                properties.getRateLimit().getBackoffInitialMs(),
                properties.getRateLimit().getBackoffMaxMs(),
                5
        );

        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(properties.getRateLimit().getRequestsPerSecond())
                .refillGreedy(properties.getRateLimit().getRequestsPerSecond(), Duration.ofSeconds(1))
                .build();
        this.globalBucket = Bucket.builder().addLimit(bandwidth).build();
    }

    public HttpResponse<byte[]> getBytes(String url) throws IOException, InterruptedException {
        return executeWithRetry(url, HttpResponse.BodyHandlers.ofByteArray());
    }

    public HttpResponse<String> getString(String url) throws IOException, InterruptedException {
        return executeWithRetry(url, HttpResponse.BodyHandlers.ofString());
    }

    private <T> HttpResponse<T> executeWithRetry(String url, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            if (!circuitBreaker.allowRequest()) {
                metrics.recordCircuitBreakerState(circuitBreaker.getState());
                throw new IOException("Circuit breaker is OPEN for URL: " + url);
            }

            waitForRateLimit(url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "EpsteinBrowser-Ingestion/1.0 (Government FOIA Document Archival)")
                    .header("Accept", "*/*")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            long start = System.nanoTime();
            try {
                HttpResponse<T> response = httpClient.send(request, handler);
                long durationMs = (System.nanoTime() - start) / 1_000_000;
                metrics.recordHttpRequest(response.statusCode(), durationMs);

                if (response.statusCode() == 200) {
                    circuitBreaker.recordSuccess();
                    return response;
                }

                if (response.statusCode() == 429 || response.statusCode() == 503) {
                    circuitBreaker.recordFailure();
                    if (retryPolicy.shouldRetry(attempt)) {
                        long delay = retryPolicy.calculateDelayMs(attempt);
                        log.warn("HTTP {} for {}. Retrying in {}ms (attempt {}/{})",
                                response.statusCode(), url, delay, attempt + 1, retryPolicy.getMaxRetries());
                        Thread.sleep(delay);
                        attempt++;
                        continue;
                    }
                }

                if (response.statusCode() >= 400) {
                    circuitBreaker.recordFailure();
                    throw new IOException("HTTP " + response.statusCode() + " for URL: " + url);
                }

                return response;
            } catch (IOException e) {
                long durationMs = (System.nanoTime() - start) / 1_000_000;
                metrics.recordHttpRequest(0, durationMs);
                circuitBreaker.recordFailure();
                if (retryPolicy.shouldRetry(attempt)) {
                    long delay = retryPolicy.calculateDelayMs(attempt);
                    log.warn("IO error for {}. Retrying in {}ms (attempt {}/{}): {}",
                            url, delay, attempt + 1, retryPolicy.getMaxRetries(), e.getMessage());
                    Thread.sleep(delay);
                    attempt++;
                } else {
                    throw e;
                }
            }
        }
    }

    private void waitForRateLimit(String url) throws InterruptedException {
        String host = URI.create(url).getHost();

        // Polite delay per host
        Long lastRequest = lastRequestByHost.get(host);
        if (lastRequest != null) {
            long elapsed = System.currentTimeMillis() - lastRequest;
            long politeDelay = properties.getRateLimit().getPoliteDelayMs();
            if (elapsed < politeDelay) {
                long wait = politeDelay - elapsed;
                metrics.recordRateLimitWait(wait);
                Thread.sleep(wait);
            }
        }

        // Global rate limit
        long waitStart = System.nanoTime();
        globalBucket.asBlocking().consume(1);
        long waited = (System.nanoTime() - waitStart) / 1_000_000;
        if (waited > 1) {
            metrics.recordRateLimitWait(waited);
        }

        // Per-host rate limit
        Bucket hostBucket = hostBuckets.computeIfAbsent(host, h -> {
            Bandwidth bandwidth = Bandwidth.builder()
                    .capacity(properties.getRateLimit().getRequestsPerSecond())
                    .refillGreedy(properties.getRateLimit().getRequestsPerSecond(), Duration.ofSeconds(1))
                    .build();
            return Bucket.builder().addLimit(bandwidth).build();
        });
        hostBucket.asBlocking().consume(1);

        lastRequestByHost.put(host, System.currentTimeMillis());
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
