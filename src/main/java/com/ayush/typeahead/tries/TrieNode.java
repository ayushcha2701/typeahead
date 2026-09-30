package com.ayush.typeahead.tries;

import java.util.*;


public class TrieNode {

    Map<Character, TrieNode> children;
    List<Suggestion> suggestions;

    TrieNode() {
        this(new HashMap<>(), new ArrayList<>());
    }

    TrieNode(Map<Character, TrieNode> children, List<Suggestion> suggestions) {
        this.children = children;
        this.suggestions = suggestions;
    }


}
