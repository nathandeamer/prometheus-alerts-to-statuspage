package com.nathandeamer.prometheustostatuspage.statuspage.configuration;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

public class StatusPageClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(@Value("${statusPageAuth}") String auth) {
        return requestTemplate -> {
            requestTemplate.header(HttpHeaders.AUTHORIZATION, auth);
        };
    }

}
