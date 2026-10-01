package com.ayush.typeahead.tries;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the top suggestions of every prefix in Redis.
 *
 *   suggest:current        -> "1700000000000"             (version readers use right now)
 *   suggest:v<version>:me  -> ["messi", "meta", ...]      (Redis list, best first)
 *
 * A publish writes a complete new version first and only then moves suggest:current to it,
 * so readers always see one consistent snapshot. Old versions expire on their own (TTL).
 */
@Component
@RequiredArgsConstructor
public class SuggestionStore {

    private static final String CURRENT_KEY = "suggest:current";
    // must outlive the rebuild interval so a reader that just read the old version can still finish
    private static final Duration TTL = Duration.ofMinutes(10);
    // how many prefixes go to Redis in one pipelined round trip
    private static final int BATCH_SIZE = 500;

    private final StringRedisTemplate redis;

    // longer prefixes are not worth precomputing; the caller falls back to the local trie
    @Value("${typeahead.suggest.max-prefix-length:10}")
    private int maxPrefixLength;

    private String key(long version, String prefix) {
        return "suggest:v" + version + ":" + prefix;
    }

    public void publish(Trie trie) {
        long version = System.currentTimeMillis();
        Map<String, List<String>> batch = new LinkedHashMap<>();

        trie.forEachPrefix((prefix, suggestions) -> {
            if (prefix.length() > maxPrefixLength) {
                return;
            }
            batch.put(key(version, prefix), termsOf(suggestions));
            if (batch.size() >= BATCH_SIZE) {
                write(batch);
                batch.clear();
            }
        });
        write(batch);

        // last on purpose: readers switch to the new snapshot only after it is fully written
        redis.opsForValue().set(CURRENT_KEY, String.valueOf(version));
    }

    // one network round trip for the whole batch instead of two per prefix
    private void write(Map<String, List<String>> batch) {
        if (batch.isEmpty()) {
            return;
        }
        RedisSerializer<String> serializer = redis.getStringSerializer();
        redis.executePipelined((RedisCallback<Object>) connection -> {
            batch.forEach((k, terms) -> {
                byte[] keyBytes = serializer.serialize(k);
                byte[][] values = terms.stream().map(serializer::serialize).toArray(byte[][]::new);
                connection.listCommands().rPush(keyBytes, values);
                connection.keyCommands().expire(keyBytes, TTL.toSeconds());
            });
            return null;
        });
    }

    private List<String> termsOf(List<Suggestion> suggestions) {
        return suggestions.stream().map(Suggestion::term).toList();
    }

    public List<String> get(String prefix) {
        String version = redis.opsForValue().get(CURRENT_KEY);
        if (version == null) {
            return List.of();
        }
        List<String> result = redis.opsForList().range(key(Long.parseLong(version), prefix), 0, -1);
        return result == null ? List.of() : result;
    }
}
