package com.nathandeamer.prometheustostatuspage.statuspage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IncidentRequestWrapper {
    @JsonProperty("incident")
    private final IncidentRequest incidentRequest;

    @JsonCreator
    public IncidentRequestWrapper(IncidentRequest incidentRequest) {
        this.incidentRequest = incidentRequest;
    }
}
