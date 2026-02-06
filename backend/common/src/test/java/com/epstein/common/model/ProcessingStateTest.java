package com.epstein.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessingStateTest {

    @Test
    void allStatesExist() {
        assertEquals(5, ProcessingState.values().length);
    }

    @Test
    void stateNames() {
        assertEquals("INGESTED", ProcessingState.INGESTED.name());
        assertEquals("EXTRACTING", ProcessingState.EXTRACTING.name());
        assertEquals("EXTRACTED", ProcessingState.EXTRACTED.name());
        assertEquals("EXTRACT_FAILED", ProcessingState.EXTRACT_FAILED.name());
        assertEquals("INDEXED", ProcessingState.INDEXED.name());
    }
}
