package com.lastminute.recruitment.domain;

import com.lastminute.recruitment.domain.error.WikiPageNotFound;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core domain service that scrapes wiki pages starting from a root link,
 * following all discovered links using BFS with cycle detection.
 */
public class WikiScrapper {

    private static final Logger LOG = Logger.getLogger(WikiScrapper.class.getName());

    private final WikiPageReader pageReader;
    private final WikiPageStore pageStore;

    public WikiScrapper(WikiPageReader pageReader, WikiPageStore pageStore) {
        this.pageReader = pageReader;
        this.pageStore = pageStore;
    }

    /**
     * Reads the wiki page at the given link and recursively follows all linked pages.
     * Pages are persisted via {@link WikiPageStore}. Already-visited links are skipped
     * to avoid infinite loops in cyclic graphs.
     *
     * @param link the root page link to start scraping from
     * @throws WikiPageNotFound if the root page does not exist
     */
    public void read(String link) {
        LOG.log(Level.INFO, "Starting scrape from: {0}", link);

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(link);
        visited.add(link);

        boolean isRoot = true;

        while (!queue.isEmpty()) {
            String currentLink = queue.poll();

            WikiPage page = pageReader.fetchPage(currentLink).orElse(null);

            if (page == null) {
                if (isRoot) {
                    LOG.log(Level.WARNING, "Root page not found: {0}", currentLink);
                    throw new WikiPageNotFound();
                }
                LOG.log(Level.WARNING, "Child page not found, skipping: {0}", currentLink);
                continue;
            }

            isRoot = false;

            pageStore.save(page);
            LOG.log(Level.INFO, "Saved page: {0}", page.getTitle());

            for (String childLink : page.getLinks()) {
                if (!visited.contains(childLink)) {
                    visited.add(childLink);
                    queue.add(childLink);
                }
            }
        }

        LOG.log(Level.INFO, "Scrape completed. Total pages visited: {0}", visited.size());
    }
}
