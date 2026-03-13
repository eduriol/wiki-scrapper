package com.lastminute.recruitment.domain;

import java.util.Optional;

/**
 * Port for fetching wiki pages by link.
 * Implementations may read from JSON files, HTML files, or any other source.
 */
public interface WikiPageReader {

    Optional<WikiPage> fetchPage(String link);
}

