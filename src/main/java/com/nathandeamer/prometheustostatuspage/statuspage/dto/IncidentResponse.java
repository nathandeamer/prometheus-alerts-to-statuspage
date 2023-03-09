package com.nathandeamer.prometheustostatuspage.statuspage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class IncidentResponse {
    private final String id;
    private final List<IncidentComponentResponse> components;
    private final String impact;
    @JsonProperty("impact_override")
    private final String impactOverride;
    private final String name;
    @JsonProperty("page_id")
    private final String pageId;
    private final String status;
}
