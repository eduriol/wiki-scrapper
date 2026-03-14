package com.lastminute.recruitment.client;

import com.lastminute.recruitment.domain.WikiPage;
import com.lastminute.recruitment.domain.WikiPageReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WikiPageReader adapter that reads wiki pages from HTML classpath resources.
 * Maps a link URL to an HTML file by extracting the last path segment
 * (e.g., "http://wikiscrapper.test/site1" → "wikiscrapper/site1.html").
 */
public class HtmlWikiClient implements WikiPageReader {

    private static final Logger LOG = Logger.getLogger(HtmlWikiClient.class.getName());
    private static final String RESOURCE_PREFIX = "wikiscrapper/";
    private static final String RESOURCE_SUFFIX = ".html";

    @Override
    public Optional<WikiPage> fetchPage(String link) {
        String resourcePath = toResourcePath(link);

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                LOG.log(Level.WARNING, "HTML resource not found: {0}", resourcePath);
                return Optional.empty();
            }

            Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");

            Optional<String> selfLink = Optional.ofNullable(doc.selectFirst("meta[selfLink]"))
                    .map(el -> el.attr("selfLink"));
            Optional<String> title = Optional.ofNullable(doc.selectFirst("h1.title"))
                    .map(Element::text);
            Optional<String> content = Optional.ofNullable(doc.selectFirst("p.content"))
                    .map(Element::text);

            if (selfLink.isEmpty() || title.isEmpty() || content.isEmpty()) {
                LOG.log(Level.SEVERE, "Missing required elements in HTML resource: {0}", resourcePath);
                return Optional.empty();
            }

            List<String> links = Optional.ofNullable(doc.selectFirst("ul.links"))
                    .map(ul -> ul.select("a[href]").stream()
                            .map(a -> a.attr("href"))
                            .toList())
                    .orElse(List.of());

            return Optional.of(new WikiPage(title.get(), content.get(), selfLink.get(), links));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error parsing HTML resource: {0}", resourcePath);
            return Optional.empty();
        }
    }

    private String toResourcePath(String link) {
        String pageName = link.substring(link.lastIndexOf('/') + 1);
        return RESOURCE_PREFIX + pageName + RESOURCE_SUFFIX;
    }
}
