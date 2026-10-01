package com.ayush.typeahead.tries;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ayush.typeahead.entity.SearchTerm;
import com.ayush.typeahead.repository.SearchTermRepository;

class SuggestionIndexTest {

    private SearchTermRepository repo;
    private SuggestionStore store;
    private SuggestionIndex index;

    private static SearchTerm term(String query, double frequency, long todayCount) {
        SearchTerm t = new SearchTerm(query);
        t.setFrequency(frequency);
        t.setTodayCount(todayCount);
        return t;
    }

    @BeforeEach
    void setUp() {
        repo = mock(SearchTermRepository.class);
        store = mock(SuggestionStore.class);
        index = new SuggestionIndex(repo, store);
    }

    @Test
    void rebuildPublishesTheFreshTrieToRedis() {
        when(repo.findAll()).thenReturn(List.of(term("messi", 5, 1000), term("mobile", 500, 0)));

        index.rebuild();

        verify(store, times(1)).publish(any(Trie.class));
    }

    @Test
    void suggestReturnsRedisResultWhenPresent() {
        when(store.get("me")).thenReturn(List.of("from-redis"));

        assertEquals(List.of("from-redis"), index.suggest("me"));
    }

    @Test
    void suggestFallsBackToLocalTrieWhenRedisIsEmpty() {
        when(repo.findAll()).thenReturn(List.of(term("messi", 5, 1000), term("mobile", 500, 0)));
        when(store.get(anyString())).thenReturn(List.of());
        index.rebuild();

        // ranked by frequency + todayCount: messi 1005 beats mobile 500
        assertEquals(List.of("messi", "mobile"), index.suggest("m"));
    }

    @Test
    void suggestFallsBackToLocalTrieWhenRedisThrows() {
        when(repo.findAll()).thenReturn(List.of(term("messi", 5, 1000)));
        when(store.get(anyString())).thenThrow(new RuntimeException("redis down"));
        index.rebuild();

        assertEquals(List.of("messi"), index.suggest("me"));
    }

    @Test
    void rebuildSurvivesRedisFailureAndKeepsLocalTrie() {
        when(repo.findAll()).thenReturn(List.of(term("messi", 5, 1000)));
        doThrow(new RuntimeException("redis down")).when(store).publish(any(Trie.class));
        when(store.get(anyString())).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> index.rebuild());
        assertEquals(List.of("messi"), index.suggest("me"));
    }

    @Test
    void unknownPrefixReturnsEmptyEverywhere() {
        when(store.get(anyString())).thenReturn(List.of());

        assertTrue(index.suggest("zzz").isEmpty());
    }
}
