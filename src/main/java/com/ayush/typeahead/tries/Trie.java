package com.ayush.typeahead.tries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


public class Trie {

    private static final int MAX_SUGGESTIONS = 5;

    private final TrieNode root = new TrieNode();

    public void insert(String term, double score){
        // start at root, but don't move root itself
        TrieNode node = root;
        for(Character ch: term.toCharArray()){
            //if not present then add to child that character and then create a new trieNode for the suggestion
            node = node.children.computeIfAbsent(ch, k ->new TrieNode());
            //how to update the score??
            updateSuggestions(node, term, score);
        }

    }
    /*
         Suggestion is a record, and records
         can't be changed after creation. So the way to "update" one is to remove it and add a fresh one. Two lines cover both the "already
         there" case and the "not there" case without an if/else.
    */
    private void updateSuggestions(TrieNode node, String term, double score) {

        //this will remove the old copy if exist
        node.suggestions.removeIf(s -> s.term().equals(term));

        //add new one with updated score
        node.suggestions.add(new Suggestion(term, score));

        //sort the suggestions based on the score
        node.suggestions.sort((a, b)->Double.compare(b.score(), a.score()));

        //this make sence but does it redundant
        while (node.suggestions.size() > MAX_SUGGESTIONS) {
            node.suggestions.remove(node.suggestions.size() - 1);
        }
    }

    public List<Suggestion> search(String prefix) {

        TrieNode node = root;

        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>(node.suggestions);
    }


    public void forEachPrefix(BiConsumer<String, List<Suggestion>> action) {
       visit(root, "", action);
    }

    private void visit(TrieNode node, String prefix, BiConsumer<String, List<Suggestion>> action){
        for(Map.Entry<Character, TrieNode> entry : node.children.entrySet()){
             char letter = entry.getKey();
             TrieNode child = entry.getValue();
             String childPrefix = prefix + letter;
             action.accept(childPrefix, List.copyOf(child.suggestions));
             visit(child, childPrefix, action);
        }
    }

}
