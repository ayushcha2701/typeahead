package com.ayush.typeahead.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class SuggestionResponse {
    private List<String> suggestions;
}
