package com.nathandeamer.prometheustostatuspage;

import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.IncidentResponse;
import com.nathandeamer.prometheustostatuspage.statuspage.StatusPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrometheusToStatusPageService {

    private final StatusPageService statusPageService;

    public void alert(AlertWrapper alertWrapper) {
        List<IncidentResponse> unresolvedIncidentsForAlert = statusPageService.getUnresolvedIncidentsForPageIdAndComponentId(alertWrapper);

        if (Status.RESOLVED == alertWrapper.getStatus()) { // The 'AlertWrapper' status will only be RESOLVED if all alerts within are also resolved.
            unresolvedIncidentsForAlert.forEach(incidentResponse -> statusPageService.resolveIncident(incidentResponse, alertWrapper));
        } else if (Status.FIRING == alertWrapper.getStatus()) {
            if (unresolvedIncidentsForAlert.isEmpty()) {
                statusPageService.createIncident(alertWrapper);
            } else {
                unresolvedIncidentsForAlert.forEach(incidentResponse -> statusPageService.updateIncident(incidentResponse, alertWrapper));
            }
        }
    }

}
