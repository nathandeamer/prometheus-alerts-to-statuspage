package com.nathandeamer.prometheustostatuspage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Alert;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.StatusPageClient;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentComponentResponse;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequest;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_COMPONENT_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_COMPONENT_STATUS;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_IMPACT_OVERRIDE;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_PAGE_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUS_PAGE_IO_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTests {

	// Alert annotation.
	private static final String STATUS_PAGE_IO_SUMMARY = "statusPageIOSummary";
	private static final String STATUS_PAGE_IO_COMPONENT_NAME = "statusPageIOComponentName";

	// Shared Test data
	private final String statusPageIOPageIdValue = "statusPageIOPageId";
	private final String statusPageIOComponentIdValue = "statusPageIOComponentId";
	private final String statusPageIOIncidentId = "statusPageIOIncidentId";
	private final String statusPageIOComponentNameValue = "Status Page Component Name";
	private final String statusPageIOSummaryValue = "Summary for Status Page";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	public ObjectMapper objectMapper;

	@MockBean
	public StatusPageClient statusPageClient;

	@Captor
	ArgumentCaptor<IncidentRequestWrapper> incidentRequestWrapperCaptor;


	@Test
	public void testShouldCreateNewStatusPageIncidentForSingleAlert() throws Exception {
		List<Alert> alerts = List.of(buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.MAJOR_OUTAGE, statusPageIOSummaryValue));
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.name(statusPageIOComponentNameValue + " - Uh oh, something has gone wrong")
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING.name().toLowerCase())
						.body("Don't panic, our amazing team of engineers has been alerted!<br><b style='color: red'>Firing</b> - " + statusPageIOSummaryValue)
						.componentIds(List.of(statusPageIOComponentIdValue))
						.components(Map.of(statusPageIOComponentIdValue, ComponentStatus.MAJOR_OUTAGE.getValue()))
						.build())
				.build();

		when(statusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue)).thenReturn(Collections.emptyList());
		when(statusPageClient.createIncident(eq(statusPageIOPageIdValue), eq(expectedRequest)))
				.thenReturn(IncidentResponse.builder().id(statusPageIOIncidentId).build());

		doPost(alertWrapper);

		verify(statusPageClient).createIncident(eq(statusPageIOPageIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() throws Exception {
		String statusPageIOSummary1 = "Alert 1 - " + statusPageIOSummaryValue;
		String statusPageIOSummary2 = "Alert 2 - " + statusPageIOSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statusPageIOSummary1),
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.name(statusPageIOComponentNameValue + " - Uh oh, something has gone wrong")
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't panic, our amazing team of engineers has been alerted!<br><b style='color: red'>Firing</b> - " + statusPageIOSummary1 + "<br><b style='color: red'>Firing</b> - " + statusPageIOSummary2)
						.componentIds(List.of(statusPageIOComponentIdValue))
						.components(Map.of(statusPageIOComponentIdValue, ComponentStatus.PARTIAL_OUTAGE.getValue()))
						.build())
				.build();

		when(statusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue)).thenReturn(Collections.emptyList());
		when(statusPageClient.createIncident(eq(statusPageIOPageIdValue), eq(expectedRequest))).thenReturn(IncidentResponse.builder().id("statusPageIOIncidentId").build());

		doPost(alertWrapper);

		verify(statusPageClient).createIncident(eq(statusPageIOPageIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverrideForOneFiringAndOneResolvedAlert() throws Exception {
		String statusPageIOSummary1 = "Alert 1 - " + statusPageIOSummaryValue;
		String statusPageIOSummary2 = "Alert 2 - " + statusPageIOSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statusPageIOSummary1),
				buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.name(statusPageIOComponentNameValue + " - Uh oh, something has gone wrong")
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't panic, our amazing team of engineers has been alerted!<br><b style='color: red'>Firing</b> - " + statusPageIOSummary1 + "<br><b style='color: green'>Resolved</b> - " + statusPageIOSummary2)
						.componentIds(List.of(statusPageIOComponentIdValue))
						.components(Map.of(statusPageIOComponentIdValue, ComponentStatus.DEGRADED_PERFORMANCE.getValue()))
						.build())
				.build();

		when(statusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue)).thenReturn(Collections.emptyList());
		when(statusPageClient.createIncident(eq(statusPageIOPageIdValue), eq(expectedRequest))).thenReturn(IncidentResponse.builder().id("statusPageIOIncidentId").build());

		doPost(alertWrapper);

		verify(statusPageClient).createIncident(eq(statusPageIOPageIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldUpdateExistingStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() throws Exception {
		String statusPageIOSummary1 = "Alert 1 - " + statusPageIOSummaryValue;
		String statusPageIOSummary2 = "Alert 2 - " + statusPageIOSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummary1),
				buildAlert(Status.FIRING, ImpactOverride.CRITICAL, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.MAJOR_OUTAGE, statusPageIOSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.impactOverride(ImpactOverride.CRITICAL.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statusPageIOSummary1 + "<br><b style='color: red'>Firing</b> - " + statusPageIOSummary2)
						.componentIds(List.of(statusPageIOComponentIdValue))
						.components(Map.of(statusPageIOComponentIdValue, ComponentStatus.MAJOR_OUTAGE.getValue()))
						.build())
				.build();

		when(statusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue)).thenReturn(List.of(
				IncidentResponse.builder()
						.id(statusPageIOIncidentId)
						.pageId(statusPageIOPageIdValue)
						.components(List.of(IncidentComponentResponse.builder()
								.id(statusPageIOComponentIdValue)
								.build()))
						.build()));

		doPost(alertWrapper);

		verify(statusPageClient).updateIncident(eq(statusPageIOPageIdValue), eq(statusPageIOIncidentId), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldUpdateExistingStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverrideForOneFiringAndOneResolvedAlert() throws Exception {
		String statusPageIOSummary1 = "Alert 1 - " + statusPageIOSummaryValue;
		String statusPageIOSummary2 = "Alert 2 - " + statusPageIOSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.PARTIAL_OUTAGE, statusPageIOSummary1),
				buildAlert(Status.RESOLVED, ImpactOverride.CRITICAL, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.MAJOR_OUTAGE, statusPageIOSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.impactOverride(ImpactOverride.CRITICAL.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statusPageIOSummary1 + "<br><b style='color: green'>Resolved</b> - " + statusPageIOSummary2)
						.componentIds(List.of(statusPageIOComponentIdValue))
						.components(Map.of(statusPageIOComponentIdValue, ComponentStatus.PARTIAL_OUTAGE.getValue()))
						.build())
				.build();

		when(statusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue)).thenReturn(List.of(
				IncidentResponse.builder()
						.id(statusPageIOIncidentId)
						.pageId(statusPageIOPageIdValue)
						.components(List.of(IncidentComponentResponse.builder()
								.id(statusPageIOComponentIdValue)
								.build()))
						.build()));

		doPost(alertWrapper);

		verify(statusPageClient).updateIncident(eq(statusPageIOPageIdValue), eq(statusPageIOIncidentId), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldResolveExistingIncidentWhenAlertWrapperIsResolved() throws Exception {
		List<Alert> alerts = List.of(buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.MAJOR_OUTAGE, statusPageIOSummaryValue));
		AlertWrapper alertWrapper = buildAlertWrapper(Status.RESOLVED, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.RESOLVED.name().toLowerCase())
						.body("Our engineers have fixed the problem. They will be doing a postmortem within the next 48 hours.<br><b style='color: green'>Resolved</b> - " + statusPageIOSummaryValue)
						.componentIds(List.of(statusPageIOComponentIdValue))
						.components(Map.of(statusPageIOComponentIdValue, ComponentStatus.OPERATIONAL.getValue()))
						.build())
				.build();

		when(statusPageClient.getUnresolvedIncidents(statusPageIOPageIdValue)).thenReturn(List.of(
				IncidentResponse.builder()
						.id(statusPageIOIncidentId)
						.pageId(statusPageIOPageIdValue)
						.components(List.of(IncidentComponentResponse.builder()
										.id(statusPageIOComponentIdValue)
								.build()))
						.build()));

		when(statusPageClient.updateIncident(eq(statusPageIOPageIdValue), eq(statusPageIOIncidentId), eq(expectedRequest)))
				.thenReturn(IncidentResponse.builder().id(statusPageIOIncidentId).build());

		doPost(alertWrapper);

		verify(statusPageClient).updateIncident(eq(statusPageIOPageIdValue), eq(statusPageIOIncidentId), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	private void doPost(AlertWrapper alertWrapper) throws Exception {
		this.mockMvc
				.perform(post("/alert")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(alertWrapper)))
				.andExpect(status().isOk());
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
