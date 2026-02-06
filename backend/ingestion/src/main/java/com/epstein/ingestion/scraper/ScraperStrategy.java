package com.epstein.ingestion.scraper;

import com.epstein.common.model.IngestionRun;

public interface ScraperStrategy {

    String getSourceType();

    void scrape(IngestionRun run);
}
