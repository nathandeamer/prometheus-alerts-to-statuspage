package com.nathandeamer.prometheustostatuspage.statuspage.dto;

import lombok.Getter;

@Getter
public enum ComponentStatus {

    NONE(""), OPERATIONAL("operational"), DEGRADED_PERFORMANCE("degraded_performance"), PARTIAL_OUTAGE("partial_outage"), MAJOR_OUTAGE("major_outage");

    private final String value;

    ComponentStatus(String value) {
        this.value = value;
    }
}
