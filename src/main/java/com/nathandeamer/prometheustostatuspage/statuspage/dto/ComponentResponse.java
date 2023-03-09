package com.nathandeamer.prometheustostatuspage.statuspage.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ComponentResponse {
    private final String id;
    private final String name;
}
