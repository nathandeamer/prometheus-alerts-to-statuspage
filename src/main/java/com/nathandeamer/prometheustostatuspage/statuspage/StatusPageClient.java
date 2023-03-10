package com.nathandeamer.prometheustostatuspage.statuspage;

import com.nathandeamer.prometheustostatuspage.statuspage.configuration.StatusPageClientConfiguration;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name="statuspage", url="${statusPageApiUrl}", configuration = StatusPageClientConfiguration.class)
public interface StatusPageClient {

    @PostMapping(value="/pages/{pageId}/incidents")
    IncidentResponse createIncident(@PathVariable("pageId") String pageId,
                                    @RequestBody IncidentRequestWrapper incidentRequestWrapper);

    @PatchMapping(value="/pages/{pageId}/incidents/{incidentId}")
    IncidentResponse updateIncident(@PathVariable("pageId") String pageId,
                                    @PathVariable("incidentId") String incidentId,
                                    @RequestBody IncidentRequestWrapper incidentRequestWrapper);

    @GetMapping(value="/pages/{pageId}/incidents/unresolved")
    List<IncidentResponse> getUnresolvedIncidents(@PathVariable("pageId") String pageId);
}