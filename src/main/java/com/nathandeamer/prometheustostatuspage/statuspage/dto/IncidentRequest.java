package com.nathandeamer.prometheustostatuspage.statuspage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IncidentRequest {
    private final String id;
    private final String name;
    @JsonProperty("impact_override")
    private final String impactOverride;
    private final String status;
    private final String body;
    private final Map<String, String> components;
    @JsonProperty("component_ids")
    private final List<String> componentIds;
}
