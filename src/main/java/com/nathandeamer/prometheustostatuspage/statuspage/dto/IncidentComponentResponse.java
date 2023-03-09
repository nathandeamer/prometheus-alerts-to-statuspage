package com.nathandeamer.prometheustostatuspage.statuspage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class IncidentComponentResponse {
    private String id;
    @JsonProperty("page_id")
    private String pageId;
    private String name;
    private String description;
    private String status;
}
