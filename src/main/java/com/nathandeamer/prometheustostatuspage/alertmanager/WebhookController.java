package com.nathandeamer.prometheustostatuspage.alertmanager;

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

    @PostMapping("/alert")
    public void alert(@RequestBody AlertWrapper alert) {
        log.debug("{}", alert);
        prometheusToStatusPageService.alert(alert);
    }

}
