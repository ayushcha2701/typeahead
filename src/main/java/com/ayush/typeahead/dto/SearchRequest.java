package com.ayush.typeahead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor 
public class SearchRequest {
    @Size(max = 255)
    @NotBlank
    private String query;
}
