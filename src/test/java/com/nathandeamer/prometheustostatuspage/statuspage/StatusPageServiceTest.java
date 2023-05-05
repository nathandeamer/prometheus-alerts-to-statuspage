package com.nathandeamer.prometheustostatuspage.statuspage;

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
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.util.List;
import java.util.Map;

import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUS_PAGE_INCIDENT_TITLE_TEMPLATE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.buildAlert;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.buildAlertWrapper;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUSPAGE_COMPONENT_ID_VALUE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUSPAGE_INCIDENT_ID_VALUE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUSPAGE_PAGE_ID_VALUE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUSPAGE_SUMMARY_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisabledInNativeImage // Known limitation with mockito when running as native tests. https://github.com/spring-projects/spring-boot/issues/32195
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StatusPageServiceTest {

    // Alert annotation.


    private final StatusPageClient mockStatusPageClient = mock(StatusPageClient.class);

    private StatusPageService underTest;

    @BeforeAll
    public void setup() {
        underTest = new StatusPageService(STATUS_PAGE_INCIDENT_TITLE_TEMPLATE, STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE, STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE, STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE, mockStatusPageClient, new HandlebarsConfiguration().handlebars());
    }

    @Test
    public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, STATUSPAGE_SUMMARY_VALUE),
                buildAlert(Status.FIRING, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, STATUSPAGE_SUMMARY_VALUE)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .name(STATUS_PAGE_INCIDENT_TITLE_TEMPLATE)
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE)
                        .componentIds(List.of(STATUSPAGE_COMPONENT_ID_VALUE))
                        .components(Map.of(STATUSPAGE_COMPONENT_ID_VALUE, ComponentStatus.PARTIAL_OUTAGE.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.createIncident(STATUSPAGE_PAGE_ID_VALUE, expectedRequest))
                .thenReturn(IncidentResponse.builder().id(STATUSPAGE_INCIDENT_ID_VALUE).build());

        underTest.createIncident(alertWrapper);

        verify(mockStatusPageClient).createIncident(STATUSPAGE_PAGE_ID_VALUE, expectedRequest);
    }

    @Test
    public void testShouldUpdateIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, STATUSPAGE_SUMMARY_VALUE),
                buildAlert(Status.RESOLVED, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, STATUSPAGE_SUMMARY_VALUE)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(STATUSPAGE_INCIDENT_ID_VALUE)
                .pageId(STATUSPAGE_PAGE_ID_VALUE)
                .build();

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE)
                        .componentIds(List.of(STATUSPAGE_COMPONENT_ID_VALUE))
                        .components(Map.of(STATUSPAGE_COMPONENT_ID_VALUE, ComponentStatus.DEGRADED_PERFORMANCE.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.updateIncident(STATUSPAGE_PAGE_ID_VALUE, STATUSPAGE_INCIDENT_ID_VALUE, expectedRequest))
                .thenReturn(incidentResponse);

        underTest.updateIncident(incidentResponse, alertWrapper);

        verify(mockStatusPageClient).updateIncident(STATUSPAGE_PAGE_ID_VALUE, STATUSPAGE_INCIDENT_ID_VALUE, expectedRequest);
    }

    @Test
    public void testShouldResolveIncident() {
        List<Alert> alerts = List.of(
                buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, STATUSPAGE_SUMMARY_VALUE),
                buildAlert(Status.RESOLVED, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, STATUSPAGE_SUMMARY_VALUE)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.RESOLVED, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(STATUSPAGE_INCIDENT_ID_VALUE)
                .pageId(STATUSPAGE_PAGE_ID_VALUE)
                .build();

        IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
                .incidentRequest(IncidentRequest.builder()
                        .impactOverride(ImpactOverride.MAJOR.name().toLowerCase())
                        .status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.RESOLVED.name().toLowerCase())
                        .body(STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE)
                        .componentIds(List.of(STATUSPAGE_COMPONENT_ID_VALUE))
                        .components(Map.of(STATUSPAGE_COMPONENT_ID_VALUE, ComponentStatus.OPERATIONAL.getValue()))
                        .build())
                .build();

        when(mockStatusPageClient.updateIncident(STATUSPAGE_PAGE_ID_VALUE, STATUSPAGE_INCIDENT_ID_VALUE, expectedRequest))
                .thenReturn(incidentResponse);

        underTest.resolveIncident(incidentResponse, alertWrapper);

        verify(mockStatusPageClient).updateIncident(STATUSPAGE_PAGE_ID_VALUE, STATUSPAGE_INCIDENT_ID_VALUE, expectedRequest);
    }

    @Test
    public void getUnresolvedIncidentsForAlertWrapper() {
        List<Alert> alerts = List.of(
                buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, STATUSPAGE_SUMMARY_VALUE),
                buildAlert(Status.FIRING, ImpactOverride.MAJOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, STATUSPAGE_SUMMARY_VALUE)
        );
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        IncidentResponse incidentResponse = IncidentResponse.builder()
                .id(STATUSPAGE_INCIDENT_ID_VALUE)
                .pageId(STATUSPAGE_PAGE_ID_VALUE)
                .components(List.of(IncidentComponentResponse.builder()
                        .id(STATUSPAGE_COMPONENT_ID_VALUE)
                        .build()))
                .build();

        when(mockStatusPageClient.getUnresolvedIncidents(STATUSPAGE_PAGE_ID_VALUE))
                .thenReturn(List.of(incidentResponse));

        List<IncidentResponse> result = underTest.getUnresolvedIncidentsForAlertWrapper(alertWrapper);

        assertEquals(1, result.size());
        assertEquals(result.get(0).getId(), STATUSPAGE_INCIDENT_ID_VALUE);
    }


}
