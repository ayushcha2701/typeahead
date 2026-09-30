package com.ayush.typeahead.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity  
@Getter 
@Setter 
@NoArgsConstructor 
public class SearchTerm {
   
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 255)
    private String searchQuery;
    private double frequency;
    private long todayCount;
    private Instant lastUpdated;

     
    public SearchTerm(String searchQuery){
        this.searchQuery = searchQuery;
        this.todayCount = 1; 
        this.lastUpdated = Instant.now();
    }
}
