package com.epstein.ingestion.service;

import com.epstein.common.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class DeduplicationService {

    private final DocumentRepository documentRepository;

    public DeduplicationService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public String computeHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public boolean isDuplicate(String fileHash) {
        return documentRepository.existsByFileHash(fileHash);
    }
}
