package com.lastminute.recruitment.client;

import com.lastminute.recruitment.domain.WikiPage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonWikiClientTest {

    private final JsonWikiClient client = new JsonWikiClient();

    @Test
    void shouldParsePageWithNoLinks() {
        // given
        String link = "http://wikiscrapper.test/site1";

        // when
        Optional<WikiPage> result = client.fetchPage(link);

        // then
        assertTrue(result.isPresent());
        WikiPage page = result.get();
        assertEquals("Site 1", page.getTitle());
        assertEquals("Content 1", page.getContent());
        assertEquals("http://wikiscrapper.test/site1", page.getSelfLink());
        assertTrue(page.getLinks().isEmpty());
    }

    @Test
    void shouldParsePageWithLinks() {
        // given
        String link = "http://wikiscrapper.test/site2";

        // when
        Optional<WikiPage> result = client.fetchPage(link);

        // then
        assertTrue(result.isPresent());
        WikiPage page = result.get();
        assertEquals("Site 2", page.getTitle());
        assertEquals("Content 2", page.getContent());
        assertEquals("http://wikiscrapper.test/site2", page.getSelfLink());
        assertEquals(List.of(
                "http://wikiscrapper.test/site4",
                "http://wikiscrapper.test/site5"
        ), page.getLinks());
    }

    @Test
    void shouldParsePageWithManyLinks() {
        // given
        String link = "http://wikiscrapper.test/site5";

        // when
        Optional<WikiPage> result = client.fetchPage(link);

        // then
        assertTrue(result.isPresent());
        WikiPage page = result.get();
        assertEquals("Site 5", page.getTitle());
        assertEquals(4, page.getLinks().size());
    }

    @Test
    void shouldReturnEmptyWhenResourceNotFound() {
        // given
        String link = "http://wikiscrapper.test/nonexistent";

        // when
        Optional<WikiPage> result = client.fetchPage(link);

        // then
        assertTrue(result.isEmpty());
    }
}

