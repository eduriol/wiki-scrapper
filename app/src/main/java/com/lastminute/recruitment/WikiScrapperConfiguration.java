package com.lastminute.recruitment;

import com.lastminute.recruitment.client.JsonWikiClient;
import com.lastminute.recruitment.domain.WikiPageReader;
import com.lastminute.recruitment.domain.WikiPageStore;
import com.lastminute.recruitment.domain.WikiScrapper;
import com.lastminute.recruitment.persistence.WikiPageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class WikiScrapperConfiguration {

    @Bean
    @Profile("json")
    public JsonWikiClient jsonWikiReader() {
        return new JsonWikiClient();
    }

    @Bean
    public WikiPageRepository wikiPageRepository() {
        return new WikiPageRepository();
    }

    @Bean
    public WikiScrapper wikiScrapper(WikiPageReader pageReader, WikiPageStore pageStore) {
        return new WikiScrapper(pageReader, pageStore);
    }
}
