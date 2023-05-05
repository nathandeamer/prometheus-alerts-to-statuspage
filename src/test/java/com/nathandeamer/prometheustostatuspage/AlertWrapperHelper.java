package com.nathandeamer.prometheustostatuspage;

import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Alert;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;

import java.util.List;
import java.util.Map;

import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_COMPONENT_STATUS;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_IMPACT_OVERRIDE;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_PAGE_ID;
import static com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService.STATUSPAGE_STATUS;

public final class AlertWrapperHelper {

    public static final String STATUSPAGE_SUMMARY = "statuspageSummary";
    public static final String STATUSPAGE_COMPONENT_NAME = "statuspageComponentName";

    public static final String STATUS_PAGE_INCIDENT_TITLE_TEMPLATE = "title";
    public static final String STATUS_PAGE_INCIDENT_CREATED_BODY_TEMPLATE = "create body";
    public static final String STATUS_PAGE_INCIDENT_UPDATED_BODY_TEMPLATE = "update body";
    public static final String STATUS_PAGE_INCIDENT_RESOLVED_BODY_TEMPLATE = "resolved body";

    // Shared Test data
    public static final String STATUSPAGE_PAGE_ID_VALUE = "statuspagePageId";
    public static final String STATUSPAGE_COMPONENT_ID_VALUE = "statuspageComponentId";
    public static final String STATUSPAGE_INCIDENT_ID_VALUE = "statuspageIncidentId";
    public static final String STATUS_PAGE_COMPONENT_NAME = "Status Page Component Name";
    public static final String STATUSPAGE_SUMMARY_VALUE = "Summary for Status Page";

    public static AlertWrapper buildAlertWrapper(Status status, List<Alert> alerts) {
        return AlertWrapper.builder()
                .status(status)
                .alerts(alerts)
                .commonLabels(Map.of(STATUSPAGE_PAGE_ID, STATUSPAGE_PAGE_ID_VALUE, STATUSPAGE_COMPONENT_ID, STATUSPAGE_COMPONENT_ID_VALUE))
                .commonAnnotations(Map.of(STATUSPAGE_COMPONENT_NAME, STATUS_PAGE_COMPONENT_NAME))
                .build();
    }

    public static Alert buildAlert(Status alertStatus, ImpactOverride impactOverride, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status statusPageStatus, ComponentStatus componentStatus, String statusPageSummary) {
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
