package com.nathandeamer.prometheustostatuspage.statuspage;

import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface StatusPageClient {

    @PostExchange("/pages/{pageId}/incidents")
    IncidentResponse createIncident(@PathVariable("pageId") String pageId,
                                    @RequestBody IncidentRequestWrapper incidentRequestWrapper);

    @PatchExchange("/pages/{pageId}/incidents/{incidentId}")
    IncidentResponse updateIncident(@PathVariable("pageId") String pageId,
                                    @PathVariable("incidentId") String incidentId,
                                    @RequestBody IncidentRequestWrapper incidentRequestWrapper);

    @GetExchange("/pages/{pageId}/incidents/unresolved")
    List<IncidentResponse> getUnresolvedIncidents(@PathVariable("pageId") String pageId);

}