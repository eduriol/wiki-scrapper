package com.lastminute.recruitment.client;

import com.lastminute.recruitment.domain.WikiPage;
import com.lastminute.recruitment.domain.WikiPageReader;

import java.util.Optional;

public class JsonWikiClient implements WikiPageReader {

    // TODO: implement JSON-based page fetching
    @Override
    public Optional<WikiPage> fetchPage(String link) {
        return Optional.empty();
    }
}
