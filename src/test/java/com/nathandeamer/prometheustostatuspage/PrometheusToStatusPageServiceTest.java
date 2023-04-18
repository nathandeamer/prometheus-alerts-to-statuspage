package com.nathandeamer.prometheustostatuspage;

import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisabledInNativeImage // Known limitation with mockito when running as native tests. https://github.com/spring-projects/spring-boot/issues/32195
public class PrometheusToStatusPageServiceTest {

    private final StatusPageService mockStatusPageService = mock(StatusPageService.class);

    private final PrometheusToStatusPageService underTest = new PrometheusToStatusPageService(mockStatusPageService);

    @Test
    public void shouldCreateIncidentWhenNoExistingIncidentForPageAndComponent() {
        AlertWrapper alertWrapper = AlertWrapper.builder()
                .status(Status.FIRING)
                .build();

        when(mockStatusPageService.getUnresolvedIncidentsForAlertWrapper(alertWrapper)).thenReturn(Collections.emptyList());

        underTest.alert(alertWrapper);

        verify(mockStatusPageService).getUnresolvedIncidentsForAlertWrapper(alertWrapper);
        verify(mockStatusPageService).createIncident(alertWrapper);
        verifyNoMoreInteractions(mockStatusPageService);
    }

    @Test
    public void shouldUpdateIncidentWhenExistingIncidentForPageAndComponent() {
        AlertWrapper alertWrapper = AlertWrapper.builder()
                .status(Status.FIRING)
                .build();

        IncidentResponse existingIncident = IncidentResponse.builder().build();
        when(mockStatusPageService.getUnresolvedIncidentsForAlertWrapper(alertWrapper)).thenReturn(List.of(existingIncident));

        underTest.alert(alertWrapper);

        verify(mockStatusPageService).getUnresolvedIncidentsForAlertWrapper(alertWrapper);
        verify(mockStatusPageService).updateIncident(existingIncident, alertWrapper);
        verifyNoMoreInteractions(mockStatusPageService);
    }

    @Test
    public void shouldResolveIncidentWhenExistingIncidentForPageAndComponent() {
        AlertWrapper alertWrapper = AlertWrapper.builder()
                .status(Status.RESOLVED)
                .build();

        IncidentResponse existingIncident = IncidentResponse.builder().build();
        when(mockStatusPageService.getUnresolvedIncidentsForAlertWrapper(alertWrapper)).thenReturn(List.of(existingIncident));

        underTest.alert(alertWrapper);

        verify(mockStatusPageService).getUnresolvedIncidentsForAlertWrapper(alertWrapper);
        verify(mockStatusPageService).resolveIncident(existingIncident, alertWrapper);
        verifyNoMoreInteractions(mockStatusPageService);
    }

}