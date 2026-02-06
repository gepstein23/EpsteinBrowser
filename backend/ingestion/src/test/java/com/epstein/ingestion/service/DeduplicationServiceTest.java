package com.epstein.ingestion.service;

import com.epstein.common.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeduplicationServiceTest {

    private DocumentRepository documentRepository;
    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        deduplicationService = new DeduplicationService(documentRepository);
    }

    @Test
    void computeHashReturnsSha256() {
        byte[] content = "hello world".getBytes();
        String hash = deduplicationService.computeHash(content);

        // SHA-256 of "hello world" is well-known
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash);
    }

    @Test
    void computeHashDifferentContentDifferentHash() {
        String hash1 = deduplicationService.computeHash("content1".getBytes());
        String hash2 = deduplicationService.computeHash("content2".getBytes());
        assertNotEquals(hash1, hash2);
    }

    @Test
    void computeHashSameContentSameHash() {
        String hash1 = deduplicationService.computeHash("same".getBytes());
        String hash2 = deduplicationService.computeHash("same".getBytes());
        assertEquals(hash1, hash2);
    }

    @Test
    void isDuplicateReturnsTrueWhenHashExists() {
        when(documentRepository.existsByFileHash("abc123")).thenReturn(true);
        assertTrue(deduplicationService.isDuplicate("abc123"));
    }

    @Test
    void isDuplicateReturnsFalseWhenHashNotExists() {
        when(documentRepository.existsByFileHash("abc123")).thenReturn(false);
        assertFalse(deduplicationService.isDuplicate("abc123"));
    }

    @Test
    void hashIsHexEncoded64Chars() {
        String hash = deduplicationService.computeHash("test".getBytes());
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }
}
