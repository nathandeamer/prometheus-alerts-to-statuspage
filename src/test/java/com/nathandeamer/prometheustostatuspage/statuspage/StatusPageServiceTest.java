package com.nathandeamer.prometheustostatuspage.statuspage;

import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Alert;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentComponentResponse;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequest;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.util.List;
import java.util.Map;

import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_STATUS;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_IMPACT_OVERRIDE;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_PAGE_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisabledInNativeImage // Known limitation with mockito when running as native tests. https://github.com/spring-projects/spring-boot/issues/32195
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StatusPageServiceTest {

    // Alert annotation.
    private static final String STATSPAGE_SUMMARY = "statuspageSummary";
    private static final String STATSPAGE_COMPONENT_NAME = "statuspageComponentName";

    private static final String STATUS_PAGE_INCIDENT_TITLE_TEMPLATE = "title";
    private static final String STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE = "create body";
    private static final String STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE = "update body";
    private static final String STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE = "resolved body";

    // Shared Test data
    private final String statuspagePageIdValue = "statuspagePageId";
    private final String statuspageComponentIdValue = "statuspageComponentId";
    private final String statuspageIncidentIdValue = "statuspageIncidentId";
    private final String statuspageComponentNameValue = "Status Page Component Name";
    private final String statuspageSummaryValue = "Summary for Status Page";

    private final StatusPageClient mockStatusPageClient = mock(StatusPageClient.class);

    private StatusPageService underTest;

    @BeforeAll
    public void setup() {
        underTest = new StatusPageService(STATUS_PAGE_INCIDENT_TITLE_TEMPLATE, STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE, STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE, STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE, mockStatusPageClient);
    }

    @Test
    public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statuspageSummaryValue),
                buildAlert(Status.FIRING, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statuspageSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .name(STATUS_PAGE_INCIDENT_TITLE_TEMPLATE)
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE)
                        .componentIds(List.of(statuspageComponentIdValue))
                        .components(Map.of(statuspageComponentIdValue, ComponentStatus.PARTIAL_OUTAGE.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.createIncident(statuspagePageIdValue, expectedRequest))
                .thenReturn(IncidentResponse.builder().id(statuspageIncidentIdValue).build());

        underTest.createIncident(alertWrapper);

        verify(mockStatusPageClient).createIncident(statuspagePageIdValue, expectedRequest);
    }

    @Test
    public void testShouldUpdateIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statuspageSummaryValue),
                buildAlert(Status.RESOLVED, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statuspageSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(statuspageIncidentIdValue)
                .pageId(statuspagePageIdValue)
                .build();

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE)
                        .componentIds(List.of(statuspageComponentIdValue))
                        .components(Map.of(statuspageComponentIdValue, ComponentStatus.DEGRADED_PERFORMANCE.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.updateIncident(statuspagePageIdValue, statuspageIncidentIdValue, expectedRequest))
                .thenReturn(incidentResponse);

        underTest.updateIncident(incidentResponse, alertWrapper);

        verify(mockStatusPageClient).updateIncident(statuspagePageIdValue, statuspageIncidentIdValue, expectedRequest);
    }

    @Test
    public void testShouldResolveIncident() {
        List<Alert> alerts = List.of(
                buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statuspageSummaryValue),
                buildAlert(Status.RESOLVED, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statuspageSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.RESOLVED, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(statuspageIncidentIdValue)
                .pageId(statuspagePageIdValue)
                .build();

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.RESOLVED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE)
                        .componentIds(List.of(statuspageComponentIdValue))
                        .components(Map.of(statuspageComponentIdValue, ComponentStatus.OPERATIONAL.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.updateIncident(statuspagePageIdValue, statuspageIncidentIdValue, expectedRequest))
                .thenReturn(incidentResponse);

        underTest.resolveIncident(incidentResponse, alertWrapper);

        verify(mockStatusPageClient).updateIncident(statuspagePageIdValue, statuspageIncidentIdValue, expectedRequest);
    }

    @Test
    public void getUnresolvedIncidentsForAlertWrapper() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statuspageSummaryValue),
                buildAlert(Status.FIRING, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statuspageSummaryValue)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(statuspageIncidentIdValue)
                .pageId(statuspagePageIdValue)
                .components(List.of(IncidentComponentResponse.builder()
                        .id(statuspageComponentIdValue)
                        .build()))
                .build();

        when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue))
                .thenReturn(List.of(incidentResponse));

        List<IncidentResponse> result = underTest.getUnresolvedIncidentsForAlertWrapper(alertWrapper);

        assertEquals(1, result.size());
        assertEquals(result.get(0).getId(), statuspageIncidentIdValue);
    }

    private AlertWrapper buildAlertWrapper(Status status, List<Alert> alerts) {
        return AlertWrapper.builder()
                .status(status)
                .alerts(alerts)
                .commonLabels(Map.of(STATUSPAGE_PAGE_ID, statuspagePageIdValue, STATUSPAGE_COMPONENT_ID, statuspageComponentIdValue))
                .commonAnnotations(Map.of(STATSPAGE_COMPONENT_NAME, statuspageComponentNameValue))
                .build();
    }

    private Alert buildAlert(Status alertStatus, ImpactOverride impactOverride, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status statusPageStatus, ComponentStatus componentStatus, String statusPageSummary) {
        return Alert.builder()
                .status(alertStatus)
                .annotations(Map.of(
                        STATUSPAGE_IMPACT_OVERRIDE, impactOverride.name().toLowerCase(),
                        STATUSPAGE_STATUS, statusPageStatus.name().toLowerCase(),
                        STATUSPAGE_COMPONENT_STATUS, componentStatus.getValue(),
                        STATSPAGE_SUMMARY, statusPageSummary
                )).build();
    }

}
