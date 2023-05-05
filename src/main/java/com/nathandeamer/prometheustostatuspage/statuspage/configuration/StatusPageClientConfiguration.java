package com.nathandeamer.prometheustostatuspage.statuspage.configuration;

import com.nathandeamer.prometheustostatuspage.statuspage.StatusPageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Slf4j
@Configuration
public class StatusPageClientConfiguration {
    @Bean
    public StatusPageClient statusPageClient(@Value("${statuspage-apiurl}") String statuspageApiUrl) {
        WebClient webClient = WebClient.builder()
                .baseUrl(statuspageApiUrl)
                .build();

        return HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .build()
                .createClient(StatusPageClient.class);
    }

}
