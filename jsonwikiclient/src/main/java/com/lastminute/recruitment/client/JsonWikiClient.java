package com.lastminute.recruitment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastminute.recruitment.domain.WikiPage;
import com.lastminute.recruitment.domain.WikiPageReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WikiPageReader adapter that reads wiki pages from JSON classpath resources.
 * Maps a link URL to a JSON file by extracting the last path segment
 * (e.g., "http://wikiscrapper.test/site1" → "wikiscrapper/site1.json").
 */
public class JsonWikiClient implements WikiPageReader {

    private static final Logger LOG = Logger.getLogger(JsonWikiClient.class.getName());
    private static final String RESOURCE_PREFIX = "wikiscrapper/";
    private static final String RESOURCE_SUFFIX = ".json";

    private final ObjectMapper objectMapper;

    public JsonWikiClient() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<WikiPage> fetchPage(String link) {
        String resourcePath = toResourcePath(link);

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                LOG.log(Level.WARNING, "JSON resource not found: {0}", resourcePath);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(input);

            Optional<String> title = Optional.ofNullable(root.get("title")).map(JsonNode::asText);
            Optional<String> content = Optional.ofNullable(root.get("content")).map(JsonNode::asText);
            Optional<String> selfLink = Optional.ofNullable(root.get("selfLink")).map(JsonNode::asText);

            if (title.isEmpty() || content.isEmpty() || selfLink.isEmpty()) {
                LOG.log(Level.SEVERE, "Missing required fields in JSON resource: {0}", resourcePath);
                return Optional.empty();
            }

            List<String> links = Optional.ofNullable(root.get("links"))
                    .filter(JsonNode::isArray)
                    .map(node -> {
                        List<String> result = new ArrayList<>();
                        node.forEach(item -> result.add(item.asText()));
                        return result;
                    })
                    .orElse(List.of());

            return Optional.of(new WikiPage(title.get(), content.get(), selfLink.get(), links));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error parsing JSON resource: {0}", resourcePath);
            return Optional.empty();
        }
    }

    private String toResourcePath(String link) {
        String pageName = link.substring(link.lastIndexOf('/') + 1);
        return RESOURCE_PREFIX + pageName + RESOURCE_SUFFIX;
    }
}
