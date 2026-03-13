package com.lastminute.recruitment.domain;

import com.lastminute.recruitment.domain.error.WikiPageNotFound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiScrapperTest {

    @Mock
    private WikiPageReader pageReader;

    @Mock
    private WikiPageStore pageStore;

    @InjectMocks
    private WikiScrapper wikiScrapper;

    @Test
    void shouldThrowWikiPageNotFoundWhenRootPageDoesNotExist() {
        // given
        when(pageReader.fetchPage("http://wikiscrapper.test/missing"))
                .thenReturn(Optional.empty());

        // when / then
        assertThrows(WikiPageNotFound.class,
                () -> wikiScrapper.read("http://wikiscrapper.test/missing"));

        verify(pageStore, never()).save(any());
    }

    @Test
    void shouldSaveSinglePageWithNoLinks() {
        // given
        WikiPage leaf = new WikiPage("Leaf", "Leaf content",
                "http://wikiscrapper.test/leaf", List.of());

        when(pageReader.fetchPage("http://wikiscrapper.test/leaf"))
                .thenReturn(Optional.of(leaf));

        // when
        wikiScrapper.read("http://wikiscrapper.test/leaf");

        // then
        verify(pageStore, times(1)).save(leaf);
    }

    @Test
    void shouldTraverseLinkedPagesAndSaveAll() {
        // given
        WikiPage pageA = new WikiPage("A", "Content A",
                "http://wikiscrapper.test/a",
                List.of("http://wikiscrapper.test/b", "http://wikiscrapper.test/c"));
        WikiPage pageB = new WikiPage("B", "Content B",
                "http://wikiscrapper.test/b", List.of());
        WikiPage pageC = new WikiPage("C", "Content C",
                "http://wikiscrapper.test/c", List.of());

        when(pageReader.fetchPage("http://wikiscrapper.test/a")).thenReturn(Optional.of(pageA));
        when(pageReader.fetchPage("http://wikiscrapper.test/b")).thenReturn(Optional.of(pageB));
        when(pageReader.fetchPage("http://wikiscrapper.test/c")).thenReturn(Optional.of(pageC));

        // when
        wikiScrapper.read("http://wikiscrapper.test/a");

        // then
        verify(pageStore).save(pageA);
        verify(pageStore).save(pageB);
        verify(pageStore).save(pageC);
        verify(pageStore, times(3)).save(any());
    }

    @Test
    void shouldHandleCyclicLinksWithoutInfiniteLoop() {
        // given
        WikiPage pageA = new WikiPage("A", "Content A",
                "http://wikiscrapper.test/a",
                List.of("http://wikiscrapper.test/b"));
        WikiPage pageB = new WikiPage("B", "Content B",
                "http://wikiscrapper.test/b",
                List.of("http://wikiscrapper.test/a")); // cycle back to A

        when(pageReader.fetchPage("http://wikiscrapper.test/a")).thenReturn(Optional.of(pageA));
        when(pageReader.fetchPage("http://wikiscrapper.test/b")).thenReturn(Optional.of(pageB));

        // when
        wikiScrapper.read("http://wikiscrapper.test/a");

        // then
        verify(pageStore, times(1)).save(pageA);
        verify(pageStore, times(1)).save(pageB);
        verify(pageStore, times(2)).save(any());
    }

    @Test
    void shouldSkipBrokenChildLinkAndContinue() {
        // given
        WikiPage root = new WikiPage("Root", "Root content",
                "http://wikiscrapper.test/root",
                List.of("http://wikiscrapper.test/broken", "http://wikiscrapper.test/valid"));
        WikiPage valid = new WikiPage("Valid", "Valid content",
                "http://wikiscrapper.test/valid", List.of());

        when(pageReader.fetchPage("http://wikiscrapper.test/root")).thenReturn(Optional.of(root));
        when(pageReader.fetchPage("http://wikiscrapper.test/broken")).thenReturn(Optional.empty());
        when(pageReader.fetchPage("http://wikiscrapper.test/valid")).thenReturn(Optional.of(valid));

        // when
        wikiScrapper.read("http://wikiscrapper.test/root");

        // then
        verify(pageStore).save(root);
        verify(pageStore).save(valid);
        verify(pageStore, times(2)).save(any());
    }

    @Test
    void shouldNotVisitSamePageTwiceInDiamondGraph() {
        // given — A -> B, A -> C, B -> D, C -> D (diamond shape)
        WikiPage pageA = new WikiPage("A", "A",
                "http://wikiscrapper.test/a",
                List.of("http://wikiscrapper.test/b", "http://wikiscrapper.test/c"));
        WikiPage pageB = new WikiPage("B", "B",
                "http://wikiscrapper.test/b",
                List.of("http://wikiscrapper.test/d"));
        WikiPage pageC = new WikiPage("C", "C",
                "http://wikiscrapper.test/c",
                List.of("http://wikiscrapper.test/d"));
        WikiPage pageD = new WikiPage("D", "D",
                "http://wikiscrapper.test/d", List.of());

        when(pageReader.fetchPage("http://wikiscrapper.test/a")).thenReturn(Optional.of(pageA));
        when(pageReader.fetchPage("http://wikiscrapper.test/b")).thenReturn(Optional.of(pageB));
        when(pageReader.fetchPage("http://wikiscrapper.test/c")).thenReturn(Optional.of(pageC));
        when(pageReader.fetchPage("http://wikiscrapper.test/d")).thenReturn(Optional.of(pageD));

        // when
        wikiScrapper.read("http://wikiscrapper.test/a");

        // then
        verify(pageStore, times(1)).save(pageD);
        verify(pageStore, times(4)).save(any());
    }
}

