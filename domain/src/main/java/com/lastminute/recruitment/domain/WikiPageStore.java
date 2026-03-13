package com.lastminute.recruitment.domain;

/**
 * Port for persisting wiki pages.
 * Implementations may store pages in a database, file system, or any other target.
 */
public interface WikiPageStore {

    void save(WikiPage wikiPage);
}

