package com.nathandeamer.prometheustostatuspage.statuspage;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentComponentResponse;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequest;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.Status;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatusPageService {

    public static final String STATUSPAGE_PAGE_ID = "statuspagePageId";
    public static final String STATUSPAGE_COMPONENT_ID = "statuspageComponentId";
    public static final String STATUSPAGE_STATUS = "statuspageStatus";
    public static final String STATUSPAGE_IMPACT_OVERRIDE = "statuspageImpactOverride";
    public static final String STATUSPAGE_COMPONENT_STATUS = "statuspageComponentStatus";

    private final StatusPageClient statusPageClient;
    private final Template statusPageIncidentTitleTemplate;
    private final Template statusPageIncidentCreatedBodyTemplate;
    private final Template statusPageIncidentUpdatedBodyTemplate;
    private final Template statusPageIncidentResolvedBodyTemplate;

    @SneakyThrows
    public StatusPageService(@Value("${statuspage-incident-title-template}") String statusPageIncidentTitleTemplate,
                             @Value("${statuspage-incident-created-body-template}") String statusPageIncidentCreatedBodyTemplate,
                             @Value("${statuspage-incident-updated-body-template}") String statusPageIncidentUpdatedBodyTemplate,
                             @Value("${statuspage-incident-resolved-body-template}") String statusPageIncidentResolvedBodyTemplate,
                             StatusPageClient statusPageClient) {

        Handlebars handlebars =  new Handlebars()
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("lower", StringHelpers.lower)
                .registerHelper("substring", StringHelpers.substring);

        this.statusPageClient = statusPageClient;
        this.statusPageIncidentTitleTemplate = handlebars.compileInline(statusPageIncidentTitleTemplate);
        this.statusPageIncidentCreatedBodyTemplate = handlebars.compileInline(statusPageIncidentCreatedBodyTemplate);
        this.statusPageIncidentUpdatedBodyTemplate = handlebars.compileInline(statusPageIncidentUpdatedBodyTemplate);
        this.statusPageIncidentResolvedBodyTemplate = handlebars.compileInline(statusPageIncidentResolvedBodyTemplate);
    }

    @SneakyThrows
    public String createIncident(AlertWrapper alertWrapper) {
        log.debug("Create incident: {}", alertWrapper);
        String pageId = alertWrapper.getCommonLabels().get(STATUSPAGE_PAGE_ID);
        String componentId = alertWrapper.getCommonLabels().get(STATUSPAGE_COMPONENT_ID);

        return statusPageClient.createIncident(pageId, IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .name(statusPageIncidentTitleTemplate.apply(alertWrapper))
                        .impactOverride(getMaxImpactOverride(alertWrapper).name().toLowerCase())
                        .status(getMaxStatus(alertWrapper).name().toLowerCase())
                        .body(statusPageIncidentCreatedBodyTemplate.apply(alertWrapper))
                        .components(Map.of(componentId, getMaxComponentStatus(alertWrapper).getValue()))
                        .componentIds(List.of(componentId))
                        .build())
                .build()).getId();
    }

    @SneakyThrows
    public void updateIncident(IncidentResponse incident, AlertWrapper alertWrapper) {
        log.debug("Update incident: {}", alertWrapper);

        String componentId = alertWrapper.getCommonLabels().get(STATUSPAGE_COMPONENT_ID);
        statusPageClient.updateIncident(incident.getPageId(), incident.getId() ,
                IncidentRequestWrapper.builder()
                        .incidentRequest(IncidentRequest.builder()
                                .impactOverride(getMaxImpactOverride(alertWrapper).name().toLowerCase())
                                .status(getMaxStatus(alertWrapper).name().toLowerCase())
                                .body(statusPageIncidentUpdatedBodyTemplate.apply(alertWrapper))
                                .components(Map.of(componentId, getMaxComponentStatus(alertWrapper).getValue()))
                                .componentIds(List.of(componentId))
                                .build())
                        .build());
    }

    @SneakyThrows
    public void resolveIncident(IncidentResponse incident, AlertWrapper alertWrapper) {
        log.debug("Resolve incident: {}", alertWrapper);

        String componentId = alertWrapper.getCommonLabels().get(STATUSPAGE_COMPONENT_ID);
        statusPageClient.updateIncident(incident.getPageId(), incident.getId() ,
                IncidentRequestWrapper.builder()
                        .incidentRequest(IncidentRequest.builder()
                                .impactOverride(getMaxImpactOverride(alertWrapper).name().toLowerCase())
                                .status(Status.RESOLVED.toString().toLowerCase())
                                .body(statusPageIncidentResolvedBodyTemplate.apply(alertWrapper))
                                .components(Map.of(componentId, ComponentStatus.OPERATIONAL.getValue()))
                                .componentIds(List.of(componentId))
                                .build())
                        .build());
    }

    public List<IncidentResponse> getUnresolvedIncidentsForAlertWrapper(AlertWrapper alertWrapper) {
        String pageId = alertWrapper.getCommonLabels().get(STATUSPAGE_PAGE_ID);
        String componentId = alertWrapper.getCommonLabels().get(STATUSPAGE_COMPONENT_ID);

        return statusPageClient.getUnresolvedIncidents(pageId).stream()
                .filter(unresolvedIncident -> unresolvedIncident.getComponents()
                        .stream()
                        .map(IncidentComponentResponse::getId)
                        .anyMatch(componentId::equals))
                .collect(Collectors.toList());
    }

    private Status getMaxStatus(AlertWrapper alertWrapper) {
        return alertWrapper.getAlerts()
                .stream()
                .map(r -> r.getAnnotations().getOrDefault(STATUSPAGE_STATUS, Status.IDENTIFIED.name()))
                .map(r -> Status.valueOf(r.toUpperCase()))
                .max(Comparator.comparing(Enum::ordinal))
                .orElse(Status.IDENTIFIED);
    }

    private ImpactOverride getMaxImpactOverride(AlertWrapper alertWrapper) {
        return alertWrapper.getAlerts()
                .stream()
                .map(r -> r.getAnnotations().getOrDefault(STATUSPAGE_IMPACT_OVERRIDE, ImpactOverride.NONE.name()))
                .map(r -> ImpactOverride.valueOf(r.toUpperCase()))
                .max(Comparator.comparing(Enum::ordinal))
                .orElse(ImpactOverride.NONE);
    }

    private ComponentStatus getMaxComponentStatus(AlertWrapper alertWrapper) {
        return alertWrapper.getAlerts()
                .stream()
                .filter(r -> r.getStatus() == com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status.FIRING)
                .map(r -> r.getAnnotations().getOrDefault(STATUSPAGE_COMPONENT_STATUS, ComponentStatus.NONE.getValue()))
                .map(r -> ComponentStatus.valueOf(r.toUpperCase()))
                .max(Comparator.comparing(Enum::ordinal))
                .orElse(ComponentStatus.NONE);
    }

}
