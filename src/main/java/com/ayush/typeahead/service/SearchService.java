package com.ayush.typeahead.service;

import java.util.ArrayList;
import java.util.List;

import com.ayush.typeahead.tries.SuggestionIndex;
import org.springframework.stereotype.Service;

import com.ayush.typeahead.repository.SearchTermRepository;
import java.util.Optional;
import com.ayush.typeahead.entity.SearchTerm;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

@Service 
@RequiredArgsConstructor 
public class SearchService {
    
    final private SearchTermRepository searchTermRepository;
    final private SuggestionIndex suggestionIndex;


    private String normalize(String str){
        return str.trim().toLowerCase();
    } 

    @Transactional 
    public void handleQuery(String query){

        String searchQuery = normalize(query);
        if(searchQuery.isEmpty()){
            return;
        }

        searchTermRepository.recordSearch(searchQuery);

    }

    public List<String> getSuggestions(String prefix){
        
        String prefixQuery = normalize(prefix);

        if(prefixQuery.isEmpty()){
            return new ArrayList<>();
        }

        List<String> topFive = suggestionIndex.suggest(prefixQuery);

        return topFive;
    }
}
