package com.ayush.typeahead.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ayush.typeahead.repository.SearchTermRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DecayScheduler {

    private static final Logger log = LoggerFactory.getLogger(DecayScheduler.class);

    final private SearchTermRepository searchTermRepository;

    @Value("${typeahead.decay.factor}")
    private double decayFactor;

    // Runs once a day at midnight: reduce the weight of the past, add today. 
    @Scheduled(cron = "${typeahead.decay.cron}")
    @Transactional
    public void applyDecay() {
        int rows = searchTermRepository.applyTimeDecay(decayFactor);
        log.info("Time decay applied with factor {} to {} rows", decayFactor, rows);
    }


}
