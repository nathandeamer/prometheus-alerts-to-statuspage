package com.nathandeamer.prometheustostatuspage.statuspage;

import com.github.jknack.handlebars.Handlebars;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Alert;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.configuration.HandlebarsConfiguration;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentComponentResponse;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequest;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_COMPONENT_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_COMPONENT_STATUS;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_IMPACT_OVERRIDE;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_PAGE_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StatusPageServiceTest {

    // Alert annotation.
    private static final String STATUS_PAGE_IO_SUMMARY = "statusPageIOSummary";
    private static final String STATUS_PAGE_IO_COMPONENT_NAME = "statusPageIOComponentName";

    private static final String STATUS_PAGE_INCIDENT_TITLE_TEMPLATE = "title";
    private static final String STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE = "create body";
    private static final String STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE = "update body";
    private static final String STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE = "resolved body";

    // Shared Test data
    private final String statusPageIOPageIdValue = "statusPageIOPageId";
    private final String statusPageIOComponentIdValue = "statusPageIOComponentId";
    private final String statusPageIOIncidentIdValue = "statusPageIOIncidentId";
    private final String statusPageIOComponentNameValue = "Status Page Component Name";
    private final String statusPageIOSummaryValue = "Summary for Status Page";

    private final StatusPageClient mockStatusPageClient = mock(StatusPageClient.class);
    private final Handlebars handlebars = new HandlebarsConfiguration().handlebars();

    private StatusPageService underTest;

    @BeforeAll
    public void setup() {
        underTest = new StatusPageService(STATUS_PAGE_INCIDENT_TITLE_TEMPLATE, STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE, STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE, STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE, mockStatusPageClient, handlebars);
    }

    @Test
    public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statusPageIOSummaryValue),
                buildAlert(Status.FIRING, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .name(STATUS_PAGE_INCIDENT_TITLE_TEMPLATE)
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE)
                        .componentIds(List.of(statusPageIOComponentIdValue))
                        .components(Map.of(statusPageIOComponentIdValue, ComponentStatus.PARTIAL_OUTAGE.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.createIncident(statusPageIOPageIdValue, expectedRequest))
                .thenReturn(IncidentResponse.builder().id(statusPageIOIncidentIdValue).build());

        underTest.createIncident(alertWrapper);

        verify(mockStatusPageClient).createIncident(statusPageIOPageIdValue, expectedRequest);
    }

    @Test
    public void testShouldUpdateIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statusPageIOSummaryValue),
                buildAlert(Status.RESOLVED, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(statusPageIOIncidentIdValue)
                .pageId(statusPageIOPageIdValue)
                .build();

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE)
                        .componentIds(List.of(statusPageIOComponentIdValue))
                        .components(Map.of(statusPageIOComponentIdValue, ComponentStatus.DEGRADED_PERFORMANCE.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.updateIncident(statusPageIOPageIdValue, statusPageIOIncidentIdValue, expectedRequest))
                .thenReturn(incidentResponse);

        underTest.updateIncident(incidentResponse, alertWrapper);

        verify(mockStatusPageClient).updateIncident(statusPageIOPageIdValue, statusPageIOIncidentIdValue, expectedRequest);
    }

    @Test
    public void testShouldResolveIncident() {
        List<Alert> alerts = List.of(
                buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statusPageIOSummaryValue),
                buildAlert(Status.RESOLVED, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.RESOLVED, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(statusPageIOIncidentIdValue)
                .pageId(statusPageIOPageIdValue)
                .build();

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.RESOLVED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE)
                        .componentIds(List.of(statusPageIOComponentIdValue))
                        .components(Map.of(statusPageIOComponentIdValue, ComponentStatus.OPERATIONAL.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.updateIncident(statusPageIOPageIdValue, statusPageIOIncidentIdValue, expectedRequest))
                .thenReturn(incidentResponse);

        underTest.resolveIncident(incidentResponse, alertWrapper);

        verify(mockStatusPageClient).updateIncident(statusPageIOPageIdValue, statusPageIOIncidentIdValue, expectedRequest);
    }

    @Test
    public void getUnresolvedIncidentsForAlertWrapper() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statusPageIOSummaryValue),
                buildAlert(Status.FIRING, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(statusPageIOIncidentIdValue)
                .pageId(statusPageIOPageIdValue)
                .components(List.of(IncidentComponentResponse.builder()
                        .id(statusPageIOComponentIdValue)
                        .build()))
                .build();

        when(mockStatusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue))
                .thenReturn(List.of(incidentResponse));

        List<IncidentResponse> result = underTest.getUnresolvedIncidentsForAlertWrapper(alertWrapper);

        assertEquals(1, result.size());
        assertEquals(result.get(0).getId(), statusPageIOIncidentIdValue);
    }

    private AlertWrapper buildAlertWrapper(Status status, List<Alert> alerts) {
        return AlertWrapper.builder()
                .status(status)
                .alerts(alerts)
                .commonLabels(Map.of(STATUS_PAGE_IO_PAGE_ID, statusPageIOPageIdValue, STATUS_PAGE_IO_COMPONENT_ID, statusPageIOComponentIdValue))
                .commonAnnotations(Map.of(STATUS_PAGE_IO_COMPONENT_NAME, statusPageIOComponentNameValue))
                .build();
    }

    private Alert buildAlert(Status alertStatus, ImpactOverride impactOverride, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status statusPageStatus, ComponentStatus componentStatus, String statusPageSummary) {
        return Alert.builder()
                .status(alertStatus)
                .annotations(Map.of(
                        STATUS_PAGE_IO_IMPACT_OVERRIDE, impactOverride.name().toLowerCase(),
                        STATUS_PAGE_IO_STATUS, statusPageStatus.name().toLowerCase(),
                        STATUS_PAGE_IO_COMPONENT_STATUS, componentStatus.getValue(),
                        STATUS_PAGE_IO_SUMMARY, statusPageSummary
                )).build();
    }

}
