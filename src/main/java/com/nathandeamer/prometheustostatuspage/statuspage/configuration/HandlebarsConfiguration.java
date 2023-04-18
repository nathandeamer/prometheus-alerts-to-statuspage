package com.nathandeamer.prometheustostatuspage.statuspage.configuration;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HandlebarsConfiguration {

    @Bean
    public Handlebars handlebars() {
        return new Handlebars()
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("lower", StringHelpers.lower)
                .registerHelper("substring", StringHelpers.substring);
    }
}
