package com.nathandeamer.prometheustostatuspage.statuspage.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class HandlebarsHintsTest {

    @Test
    void shouldRegisterHints() {
        RuntimeHints hints = new RuntimeHints();
        new HandlebarsHints().registerHints(hints, getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource().forResource("helpers.nashorn.js")).accepts(hints);
    }

}