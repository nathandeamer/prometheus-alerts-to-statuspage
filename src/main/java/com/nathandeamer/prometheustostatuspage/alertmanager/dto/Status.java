package com.nathandeamer.prometheustostatuspage.alertmanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
    @JsonProperty("resolved") RESOLVED,
    @JsonProperty("firing") FIRING;
}
