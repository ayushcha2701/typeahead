package com.ayush.typeahead.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayush.typeahead.entity.SearchTerm;

public interface SearchTermRepository  extends JpaRepository<SearchTerm, Long>{
    
     /**
      * Time decay: shrink the running score, then fold in today's hits.
      *   newFrequency = (frequency / factor) + todayCount
      * Done as one bulk UPDATE so we never load the rows into memory.
      */
     @Modifying
     @Query(value = """
             UPDATE search_term
             SET frequency = (frequency / :factor) + today_count,
                 today_count = 0,
                 last_updated = now()
             """, nativeQuery = true)
     int applyTimeDecay(@Param("factor") double factor);

     /**
      * Called on every user search (not scheduled).
      * Inserts the term with today_count = 1, or if it already exists, adds 1 to today_count.
      * Done as one atomic upsert in the database so that concurrent searches for the same term
      * don't lose updates (two requests both reading 10 and writing 11) and don't fail on
      * the unique constraint when both try to insert a brand-new term.
      * frequency starts at 0: this search lives in today_count and is folded into
      * frequency later by applyTimeDecay, so it is not counted twice.
      */
     @Modifying
     @Query(value = """
        INSERT INTO search_term(search_query, frequency, today_count, last_updated)
        VALUES(:query, 0, 1, now())
            ON CONFLICT(search_query)
                DO UPDATE SET today_count = search_term.today_count+1,
                    last_updated = now()
    """,nativeQuery = true
     )
     void recordSearch(@Param("query") String query);
}
