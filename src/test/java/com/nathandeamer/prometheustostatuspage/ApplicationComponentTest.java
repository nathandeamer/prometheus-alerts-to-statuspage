package com.nathandeamer.prometheustostatuspage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Alert;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentRequestWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_STATUS;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_IMPACT_OVERRIDE;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_PAGE_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: Having real problems with this test when running in nativeTest
// https://github.com/abhisheksr01/spring-boot-microservice-best-practices/blob/60fe1bcc22302c54d36c4970720978e69c8cc90a/src/test/java/com/uk/companieshouse/e2e/WireMockService.java


@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"statuspage-apiurl: http://localhost:9876"}) // Same port as test container
@Testcontainers
public class ApplicationComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Alert annotation.
    private static final String STATUSPAGE_SUMMARY = "statuspageSummary";
    private static final String STATUSPAGE_COMPONENT_NAME = "statuspageComponentName";

    // Shared Test data
    private final String statuspagePageIdValue = "statuspagePageId";
    private final String statuspageComponentIdValue = "statuspageComponentId";
    private final String statuspageIncidentIdValue = "statuspageIncidentId";
    private final String statuspageComponentNameValue = "Status Page Component Name";
    private final String statuspageSummaryValue = "Summary for Status Page";

    @Container
    private static GenericContainer wiremock = new GenericContainer("wiremock:2.35.0")
            .withExposedPorts(9876);
            //.withClasspathResourceMapping(, "/home/wiremock", BindMode.READ_ONLY)


    @Test
    public void testMyEndpoint() {
        // Start Wiremock server
        wiremock.start();

        System.out.println("I made it!");

//        // Create a WireMock stub
//        WireMock.stubFor(get(urlEqualTo("/my-endpoint"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withBody("Hello, world!")));
//
//        // Make a request to the endpoint
//        String response = new MyClient().getMyEndpoint();
//
//        // Assert the response
//        assertEquals("Hello, world!", response);

        // Stop Wiremock server
        wiremock.stop();
    }

//    @Test
//    @Tag("ComponentTest")
//    public void createIncident() throws Exception {
////        stubFor(WireMock.get(urlPathMatching("/pages/.*/incidents/unresolved"))
////                .willReturn(aResponse()
////                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
////                        .withBody(objectMapper.writeValueAsString(Collections.emptyList()))));
////
////        StubMapping createIncidentStub = stubFor(WireMock.post(urlPathMatching("/pages/.*/incidents")).willReturn(WireMock.ok()));
////
////        List<Alert> alerts = List.of(buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.MAJOR_OUTAGE, statuspageSummaryValue));
////        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);
////        doPost(alertWrapper);
////
////        List<ServeEvent> serveEventsForPost = getAllServeEvents(ServeEventQuery.forStubMapping(createIncidentStub.getId()));
////
////        assertThat(serveEventsForPost).hasSize(1);
////
////        assertThat(objectMapper.readValue(serveEventsForPost.get(0).getRequest().getBodyAsString(), IncidentRequestWrapper.class).getIncidentRequest().getBody())
////                .isEqualTo("Don't worry, our team of engineers are investigating!<br><b style='color: red'>Firing</b> - Summary for Status Page");
//    }

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