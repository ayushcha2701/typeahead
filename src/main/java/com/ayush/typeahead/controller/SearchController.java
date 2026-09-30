package com.ayush.typeahead.controller;

import org.springframework.web.bind.annotation.RestController;

import com.ayush.typeahead.dto.SearchRequest;
import com.ayush.typeahead.dto.SuggestionResponse;
import com.ayush.typeahead.service.SearchService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus; 


@RestController 
@RequiredArgsConstructor 
@RequestMapping("/api")
@CrossOrigin
@Validated
public class SearchController {
   
    final private SearchService searchService;

    @PostMapping("/search")
    public ResponseEntity<Void> handleQuery(@Valid @RequestBody SearchRequest req) {
        searchService.handleQuery(req.getQuery());
        return new ResponseEntity<>((Void)null ,HttpStatus.ACCEPTED);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<SuggestionResponse> getSuggestions(@RequestParam @NotBlank @Size(max = 50) String prefix) {
        return new ResponseEntity<>(new SuggestionResponse(searchService.getSuggestions(prefix)), HttpStatus.OK);
    }

}
