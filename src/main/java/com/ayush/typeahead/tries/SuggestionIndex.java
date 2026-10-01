package com.ayush.typeahead.tries;
/*
 *Build the trie from the DB
 *Publish it to Redis
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

    private static final Logger log = LoggerFactory.getLogger(SuggestionIndex.class);

    private final SearchTermRepository searchTermRepository;
    private final SuggestionStore store;

    //volatile because trie can be changed by one thread and read by other, so every read must get the latest value from the main memory then cached copy
    // Kept even with Redis: it is the fallback when Redis is down or has no data for a prefix yet
    private volatile Trie trie = new Trie();


    //It runs once when the app starts, then again 60 seconds after each run finishes
    @Scheduled(fixedDelayString = "${typeahead.trie.rebuild-ms}")
    public void rebuild() {

        Trie fresh = new Trie();
        int count = 0;
        for (SearchTerm searchTerm : searchTermRepository.findAll()) {
            fresh.insert(searchTerm.getSearchQuery(), searchTerm.getFrequency() + searchTerm.getTodayCount());
            count++;
        }
        //this will switch local readers to the new data
        trie = fresh;

        // a Redis outage must not break the scheduled job: the local trie is already up to date
        try {
            store.publish(fresh);
        } catch (RuntimeException e) {
            log.warn("Could not publish suggestions to Redis", e);
        }
        log.info("rebuild with {} terms", count);
    }

    public List<String> suggest(String prefix) {
        try {
            List<String> fromRedis = store.get(prefix);
            if (!fromRedis.isEmpty()) {
                return fromRedis;
            }
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, using local trie", e);
        }
        // Redis failed, is still empty after startup, or the prefix is longer than we precompute
        return trie.search(prefix).stream().map(Suggestion::term).toList();
    }
}
