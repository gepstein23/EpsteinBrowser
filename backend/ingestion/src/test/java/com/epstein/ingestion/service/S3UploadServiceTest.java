package com.epstein.ingestion.service;

import com.epstein.ingestion.config.IngestionProperties;
import com.epstein.ingestion.metrics.IngestionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3UploadServiceTest {

    private S3Client mockS3Client;
    private IngestionProperties properties;
    private S3UploadService s3UploadService;

    @BeforeEach
    void setUp() {
        mockS3Client = mock(S3Client.class);
        properties = new IngestionProperties();
        properties.getS3().setBucket("test-bucket");
        properties.getS3().setRawPrefix("raw");
        IngestionMetrics metrics = new IngestionMetrics(new SimpleMeterRegistry());
        s3UploadService = new S3UploadService(mockS3Client, properties, metrics);
    }

    @Test
    void uploadReturnsCorrectS3Key() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = s3UploadService.upload("content".getBytes(), "data-set-1", "test.pdf");

        assertEquals("raw/data-set-1/test.pdf", key);
    }

    @Test
    void uploadCallsS3WithCorrectParams() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        s3UploadService.upload("content".getBytes(), "data-set-5", "doc.pdf");

        verify(mockS3Client).putObject(
                argThat((PutObjectRequest req) ->
                        req.bucket().equals("test-bucket") &&
                                req.key().equals("raw/data-set-5/doc.pdf") &&
                                req.contentType().equals("application/pdf")),
                any(RequestBody.class)
        );
    }

    @Test
    void uploadThrowsOnS3Error() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThrows(RuntimeException.class, () ->
                s3UploadService.upload("content".getBytes(), "data-set-1", "test.pdf"));
    }
}
