package com.nathandeamer.prometheustostatuspage.alertmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nathandeamer.prometheustostatuspage.PrometheusToStatusPageService;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final PrometheusToStatusPageService prometheusToStatusPageService;

    private final ObjectMapper objectMapper;

    @PostMapping("/alert")
    public void alert(@RequestBody AlertWrapper alert) throws Exception {
        log.info("{}", objectMapper.writeValueAsString(alert));
        prometheusToStatusPageService.alert(alert);
    }

}
