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
import org.junit.jupiter.api.condition.DisabledInNativeImage;
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

import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_STATUS;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_IMPACT_OVERRIDE;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_PAGE_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisabledInNativeImage // Known limitation with mockito when running as native tests. https://github.com/spring-projects/spring-boot/issues/32195
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTests {

	// Alert annotation.
	private static final String STATUSPAGE_SUMMARY = "statuspageSummary";
	private static final String STATUSPAGE_COMPONENT_NAME = "statuspageComponentName";

	// Shared Test data
	private final String statuspagePageIdValue = "statuspagePageId";
	private final String statuspageComponentIdValue = "statuspageComponentId";
	private final String statuspageIncidentIdValue = "statuspageIncidentId";
	private final String statuspageComponentNameValue = "Status Page Component Name";
	private final String statuspageSummaryValue = "Summary for Status Page";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	public ObjectMapper objectMapper;

	@MockBean
	public StatusPageClient mockStatusPageClient;

	@Captor
	ArgumentCaptor<IncidentRequestWrapper> incidentRequestWrapperCaptor;

	@Test
	public void testShouldCreateNewStatusPageIncidentForSingleAlert() throws Exception {
		List<Alert> alerts = List.of(buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.MAJOR_OUTAGE, statuspageSummaryValue));
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.name(statuspageComponentNameValue + " - Uh oh, something has gone wrong")
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statuspageSummaryValue)
						.componentIds(List.of(statuspageComponentIdValue))
						.components(Map.of(statuspageComponentIdValue, ComponentStatus.MAJOR_OUTAGE.getValue()))
						.build())
				.build();

		when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue)).thenReturn(Collections.emptyList());
		when(mockStatusPageClient.createIncident(eq(statuspagePageIdValue), eq(expectedRequest)))
				.thenReturn(IncidentResponse.builder().id(statuspageIncidentIdValue).build());

		doPost(alertWrapper);

		verify(mockStatusPageClient).createIncident(eq(statuspagePageIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() throws Exception {
		String statuspageSummary1 = "Alert 1 - " + statuspageSummaryValue;
		String statuspageSummary2 = "Alert 2 - " + statuspageSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statuspageSummary1),
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statuspageSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.name(statuspageComponentNameValue + " - Uh oh, something has gone wrong")
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statuspageSummary1 + "<br><b style='color: red'>Firing</b> - " + statuspageSummary2)
						.componentIds(List.of(statuspageComponentIdValue))
						.components(Map.of(statuspageComponentIdValue, ComponentStatus.PARTIAL_OUTAGE.getValue()))
						.build())
				.build();

		when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue)).thenReturn(Collections.emptyList());
		when(mockStatusPageClient.createIncident(eq(statuspagePageIdValue), eq(expectedRequest))).thenReturn(IncidentResponse.builder().id(statuspageIncidentIdValue).build());

		doPost(alertWrapper);

		verify(mockStatusPageClient).createIncident(eq(statuspagePageIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldCreateNewStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverrideForOneFiringAndOneResolvedAlert() throws Exception {
		String statuspageSummary1 = "Alert 1 - " + statuspageSummaryValue;
		String statuspageSummary2 = "Alert 2 - " + statuspageSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.DEGRADED_PERFORMANCE, statuspageSummary1),
				buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.PARTIAL_OUTAGE, statuspageSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.name(statuspageComponentNameValue + " - Uh oh, something has gone wrong")
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statuspageSummary1 + "<br><b style='color: green'>Resolved</b> - " + statuspageSummary2)
						.componentIds(List.of(statuspageComponentIdValue))
						.components(Map.of(statuspageComponentIdValue, ComponentStatus.DEGRADED_PERFORMANCE.getValue()))
						.build())
				.build();

		when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue)).thenReturn(Collections.emptyList());
		when(mockStatusPageClient.createIncident(eq(statuspagePageIdValue), eq(expectedRequest))).thenReturn(IncidentResponse.builder().id(statuspageIncidentIdValue).build());

		doPost(alertWrapper);

		verify(mockStatusPageClient).createIncident(eq(statuspagePageIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldUpdateExistingStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverride() throws Exception {
		String statuspageSummary1 = "Alert 1 - " + statuspageSummaryValue;
		String statuspageSummary2 = "Alert 2 - " + statuspageSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.PARTIAL_OUTAGE, statuspageSummary1),
				buildAlert(Status.FIRING, ImpactOverride.CRITICAL, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.MAJOR_OUTAGE, statuspageSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.impactOverride(ImpactOverride.CRITICAL.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statuspageSummary1 + "<br><b style='color: red'>Firing</b> - " + statuspageSummary2)
						.componentIds(List.of(statuspageComponentIdValue))
						.components(Map.of(statuspageComponentIdValue, ComponentStatus.MAJOR_OUTAGE.getValue()))
						.build())
				.build();

		when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue)).thenReturn(List.of(
				IncidentResponse.builder()
						.id(statuspageIncidentIdValue)
						.pageId(statuspagePageIdValue)
						.components(List.of(IncidentComponentResponse.builder()
								.id(statuspageComponentIdValue)
								.build()))
						.build()));

		doPost(alertWrapper);

		verify(mockStatusPageClient).updateIncident(eq(statuspagePageIdValue), eq(statuspageIncidentIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldUpdateExistingStatusPageIncidentForGroupedAlertWithCorrectMaxStatusAndMaxImpactOverrideForOneFiringAndOneResolvedAlert() throws Exception {
		String statuspageSummary1 = "Alert 1 - " + statuspageSummaryValue;
		String statuspageSummary2 = "Alert 2 - " + statuspageSummaryValue;

		List<Alert> alerts = List.of(
				buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.PARTIAL_OUTAGE, statuspageSummary1),
				buildAlert(Status.RESOLVED, ImpactOverride.CRITICAL, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED, ComponentStatus.MAJOR_OUTAGE, statuspageSummary2)
		);
		AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.impactOverride(ImpactOverride.CRITICAL.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.IDENTIFIED.name().toLowerCase())
						.body("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - " + statuspageSummary1 + "<br><b style='color: green'>Resolved</b> - " + statuspageSummary2)
						.componentIds(List.of(statuspageComponentIdValue))
						.components(Map.of(statuspageComponentIdValue, ComponentStatus.PARTIAL_OUTAGE.getValue()))
						.build())
				.build();

		when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue)).thenReturn(List.of(
				IncidentResponse.builder()
						.id(statuspageIncidentIdValue)
						.pageId(statuspagePageIdValue)
						.components(List.of(IncidentComponentResponse.builder()
								.id(statuspageComponentIdValue)
								.build()))
						.build()));

		doPost(alertWrapper);

		verify(mockStatusPageClient).updateIncident(eq(statuspagePageIdValue), eq(statuspageIncidentIdValue), incidentRequestWrapperCaptor.capture());

		assertEquals(expectedRequest, incidentRequestWrapperCaptor.getValue());
	}

	@Test
	public void testShouldResolveExistingIncidentWhenAlertWrapperIsResolved() throws Exception {
		List<Alert> alerts = List.of(buildAlert(Status.RESOLVED, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.MAJOR_OUTAGE, statuspageSummaryValue));
		AlertWrapper alertWrapper = buildAlertWrapper(Status.RESOLVED, alerts);

		IncidentRequestWrapper expectedRequest = IncidentRequestWrapper.builder()
				.incidentRequest(IncidentRequest.builder()
						.impactOverride(ImpactOverride.MINOR.name().toLowerCase())
						.status(com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.RESOLVED.name().toLowerCase())
						.body("Our engineers have fixed the problem. They will be doing a postmortem within the next 48 hours.<br><b style='color: green'>Resolved</b> - " + statuspageSummaryValue)
						.componentIds(List.of(statuspageComponentIdValue))
						.components(Map.of(statuspageComponentIdValue, ComponentStatus.OPERATIONAL.getValue()))
						.build())
				.build();

		when(mockStatusPageClient.getUnresolvedIncidents(statuspagePageIdValue)).thenReturn(List.of(
				IncidentResponse.builder()
						.id(statuspageIncidentIdValue)
						.pageId(statuspagePageIdValue)
						.components(List.of(IncidentComponentResponse.builder()
										.id(statuspageComponentIdValue)
								.build()))
						.build()));

		when(mockStatusPageClient.updateIncident(eq(statuspagePageIdValue), eq(statuspageIncidentIdValue), eq(expectedRequest)))
				.thenReturn(IncidentResponse.builder().id(statuspageIncidentIdValue).build());

		doPost(alertWrapper);

		verify(mockStatusPageClient).updateIncident(eq(statuspagePageIdValue), eq(statuspageIncidentIdValue), incidentRequestWrapperCaptor.capture());

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
				.commonLabels(Map.of(STATUSPAGE_PAGE_ID, statuspagePageIdValue, STATUSPAGE_COMPONENT_ID, statuspageComponentIdValue))
				.commonAnnotations(Map.of(STATUSPAGE_COMPONENT_NAME, statuspageComponentNameValue))
				.build();
	}

	private Alert buildAlert(Status alertStatus, ImpactOverride impactOverride, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status statusPageStatus, ComponentStatus componentStatus, String statusPageSummary) {
		return Alert.builder()
				.status(alertStatus)
				.annotations(Map.of(
						STATUSPAGE_IMPACT_OVERRIDE, impactOverride.name().toLowerCase(),
						STATUSPAGE_STATUS, statusPageStatus.name().toLowerCase(),
						STATUSPAGE_COMPONENT_STATUS, componentStatus.getValue(),
						STATUSPAGE_SUMMARY, statusPageSummary
				)).build();
	}

}
