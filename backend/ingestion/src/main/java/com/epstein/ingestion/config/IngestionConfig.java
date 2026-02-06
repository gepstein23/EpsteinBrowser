package com.epstein.ingestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class IngestionConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.create();
    }

    @Bean(name = "downloadExecutor", destroyMethod = "shutdown")
    public ExecutorService downloadExecutor(IngestionProperties properties) {
        return Executors.newFixedThreadPool(properties.getConcurrency().getMaxDownloads());
    }
}
