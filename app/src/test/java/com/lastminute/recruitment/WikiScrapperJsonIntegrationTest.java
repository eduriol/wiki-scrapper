package com.lastminute.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("json")
class WikiScrapperJsonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn204WhenScrapingValidLink() throws Exception {
        // given
        String validLink = "http://wikiscrapper.test/site1";

        // when / then
        mockMvc.perform(post("/wiki/scrap")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(validLink))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn204WhenScrapingPageWithLinks() throws Exception {
        // given
        String linkWithChildren = "http://wikiscrapper.test/site5";

        // when / then
        mockMvc.perform(post("/wiki/scrap")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(linkWithChildren))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenRootPageNotFound() throws Exception {
        // given
        String invalidLink = "http://wikiscrapper.test/nonexistent";

        // when / then
        mockMvc.perform(post("/wiki/scrap")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(invalidLink))
                .andExpect(status().isNotFound());
    }
}

