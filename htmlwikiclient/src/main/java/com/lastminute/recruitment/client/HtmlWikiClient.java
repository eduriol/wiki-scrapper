package com.lastminute.recruitment.client;

import com.lastminute.recruitment.domain.WikiPage;
import com.lastminute.recruitment.domain.WikiPageReader;

import java.util.Optional;

public class HtmlWikiClient implements WikiPageReader {

    // TODO: implement HTML-based page fetching
    @Override
    public Optional<WikiPage> fetchPage(String link) {
        return Optional.empty();
    }
}
