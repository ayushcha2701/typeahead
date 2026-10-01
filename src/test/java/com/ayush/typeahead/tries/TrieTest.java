package com.ayush.typeahead.tries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrieTest {

    private static List<String> terms(List<Suggestion> suggestions) {
        return suggestions.stream().map(Suggestion::term).toList();
    }

    @Test
    void shortPrefixReturnsAllMatchesBestFirst() {
        Trie trie = new Trie();
        trie.insert("messi", 100);
        trie.insert("mobile", 80);
        trie.insert("movie", 60);

        assertEquals(List.of("messi", "mobile", "movie"), terms(trie.search("m")));
    }

    @Test
    void longerPrefixNarrowsResults() {
        Trie trie = new Trie();
        trie.insert("messi", 100);
        trie.insert("mobile", 80);

        assertEquals(List.of("messi"), terms(trie.search("me")));
    }

    @Test
    void reinsertingWithHigherScoreMovesTermUpWithoutDuplicate() {
        Trie trie = new Trie();
        trie.insert("mobile", 80);
        trie.insert("messi", 50);
        trie.insert("messi", 120);   // more searches came in

        assertEquals(List.of("messi", "mobile"), terms(trie.search("m")));
    }

    @Test
    void reinsertingWithLowerScoreMovesTermDown() {
        Trie trie = new Trie();
        trie.insert("messi", 100);
        trie.insert("mobile", 80);
        trie.insert("messi", 10);    // e.g. after time decay shrank the score

        assertEquals(List.of("mobile", "messi"), terms(trie.search("m")));
    }

    @Test
    void keepsOnlyTopFive() {
        Trie trie = new Trie();
        for (int i = 1; i <= 7; i++) {
            trie.insert("m" + i, i);
        }

        assertEquals(List.of("m7", "m6", "m5", "m4", "m3"), terms(trie.search("m")));
    }

    @Test
    void unknownPrefixReturnsEmpty() {
        Trie trie = new Trie();
        trie.insert("messi", 100);

        assertTrue(trie.search("zzz").isEmpty());
    }

    @Test
    void forEachPrefixVisitsEveryPrefixWithItsTopList() {
        Trie trie = new Trie();
        trie.insert("messi", 100);
        trie.insert("mobile", 80);

        Map<String, List<String>> seen = new HashMap<>();
        trie.forEachPrefix((prefix, list) -> seen.put(prefix, terms(list)));

        assertEquals(List.of("messi", "mobile"), seen.get("m"));
        assertEquals(List.of("messi"), seen.get("me"));
        assertEquals(List.of("mobile"), seen.get("mo"));
        assertTrue(seen.containsKey("messi"));
        assertTrue(seen.containsKey("mobile"));
        assertEquals(10, seen.size());   // m + 4 more for messi + 5 more for mobile
    }

    @Test
    void forEachPrefixGivesReadOnlyLists() {
        Trie trie = new Trie();
        trie.insert("messi", 100);

        trie.forEachPrefix((prefix, list) ->
                assertThrows(UnsupportedOperationException.class, () -> list.clear()));
        assertEquals(List.of("messi"), terms(trie.search("m")));
    }
}
