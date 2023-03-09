package com.nathandeamer.prometheustostatuspage.alertmanager.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class Alert {
    private final Status status;
    private final Map<String, String> labels;
    private final Map<String, String> annotations;
    private final String startsAt;
    private final String endsAt;
    private final String generatorURL;
    private final String fingerprint;
}
