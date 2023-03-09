package com.nathandeamer.prometheustostatuspage.alertmanager.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class AlertWrapper {
    private final String version;
    private final String groupKey;
    private final int truncatedAlerts;
    private final Status status;
    private final String receiver;
    private final Map<String, String> groupLabels;
    private final Map<String, String> commonLabels;
    private final Map<String, String> commonAnnotations;
    private final String externalURL;
    private final List<Alert> alerts;
}
