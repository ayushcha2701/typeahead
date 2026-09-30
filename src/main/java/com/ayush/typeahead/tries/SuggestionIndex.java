package com.ayush.typeahead.tries;
/*
  *Build the trie from the DB
  *Keep it fresh
*/

import com.ayush.typeahead.entity.SearchTerm;
import com.ayush.typeahead.repository.SearchTermRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SuggestionIndex {

    private final SearchTermRepository searchTermRepository;
    private static final Logger log = LoggerFactory.getLogger(SuggestionIndex.class);
    //volatile because trie can be changed by one thread and read by other, so every read must get the latest value from the main memory then cached copy
    private volatile Trie trie = new Trie();


    //It runs once when the app starts, then again 60 seconds after each run finishes
    @Scheduled(fixedDelayString = "${typeahead.trie.rebuild-ms}")
    public void rebuild(){

        Trie fresh = new Trie();
        int count = 0;
        for(SearchTerm searchTerm : searchTermRepository.findAll()){
            fresh.insert(searchTerm.getSearchQuery(), searchTerm.getFrequency()+searchTerm.getTodayCount());
            count++;
        }
        //this will switch readers to the new data
        trie = fresh;
        log.info("rebuild with {} terms", count);
    }

    public List<String> suggest(String prefix){
        return trie.search(prefix).stream().map(suggestion -> suggestion.term()).toList();
    }
}
