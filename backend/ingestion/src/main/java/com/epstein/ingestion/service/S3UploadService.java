package com.epstein.ingestion.service;

import com.epstein.ingestion.config.IngestionProperties;
import com.epstein.ingestion.metrics.IngestionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3UploadService {

    private static final Logger log = LoggerFactory.getLogger(S3UploadService.class);

    private final S3Client s3Client;
    private final IngestionProperties properties;
    private final IngestionMetrics metrics;

    public S3UploadService(S3Client s3Client, IngestionProperties properties, IngestionMetrics metrics) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.metrics = metrics;
    }

    public String upload(byte[] content, String dataSet, String fileName) {
        String key = properties.getS3().getRawPrefix() + "/" + dataSet + "/" + fileName;
        String bucket = properties.getS3().getBucket();

        long start = System.nanoTime();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            metrics.recordS3Upload(durationMs);

            log.info("Uploaded to s3://{}/{} ({} bytes, {}ms)", bucket, key, content.length, durationMs);
            return key;
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            metrics.recordS3Upload(durationMs);
            throw new RuntimeException("Failed to upload to S3: " + key, e);
        }
    }
}
