# Prometheus Alerts to StatusPage.io
Do you have multiple alerts firing at once from Prometheus alertmanager that only effect one part of your system? And you only want to report one incident on a statuspage.io?  Then give this opinionated solution a go!

## Example:
### Scenario: Ecommerce - Checkout
A problem with the checkout part of your system could be alerted in multiple ways to multiple teams:
- Team 1: A synthetic E2E UI checkout test could fail
- Team 2: A synthetic API test could fail.
- Team 3: A drop off in the number of orders being created
- Team 3: A drop off in the number of baskets being created.

But the underlying cause for most of these alerts is probably the same - e.g. a database problem!

## Prerequisites
1. A [statuspage.io](https://www.atlassian.com/software/statuspage) account
2. Page and Components configured in status page
3. Your [statuspage.io](https://www.atlassian.com/software/statuspage) API key

## Usage:
### Prometheus alerts
Configure your prometheus alerts with the `statuePageIO` labels and annotations

```yaml
- alert: P1-Team1-Checkout-Customer-Alert1
  expr: vector(0) > 0
  labels:
    statusPageIO: true # Used for alert route to send to the status page
    statusPageIOPageId: lgbxhqm818kw # The status page id you want to update (from statuspage.io)
    statusPageIOComponentId: 818336q5bjxv # The status page component you want to update (from statuspage.io)
  annotations:
    statusPageIOComponentName: Checkout (Customer) # Used for incident title on status page.
    statusPageIOInitialStatus: identified # identified|investigating|monitoring|resolved
    statusPageIOImpactOverride: minor  # none|maintenance|minor|major|critical
    statusPageIOComponentStatus: partial_outage # none|operational|under_maintenance|degraded_performance|partial_outage|major_outage
    statusPageIOSummary: Customers are currently unable to checkout # Used for display text on status page
```

### AlertManager configuration
Configure your alert webhook route to group by `statusPageIOPageId` and `statusPageIOComponentId`.
```yaml
- receiver: statuspage-webhook
groupBy: ['statusPageIOPageId', 'statusPageIOComponentId']  
groupWait: 30s # Initial wait to group any other alerts which may trigger for the same group. (Default: 30s)
groupInterval: 1m # Don't send alert about new alerts in group for x (Default: 5m)
repeatInterval: 4h # Only resend the alert after x (Default: 4h)
matchers:
  - name: statusPageIO
    value: "true"
...
- name: statuspage-webhook
  webhookConfigs:
    - url: "http://prometheus-alerts-to-statuspage.default.svc.cluster.local:8080/alert"
```

## How it works
When an alert is triggered for the group, `prometheus-alerts-to-statuspage` will either create a new incident or update an existing incident depending if there is already one open.  
Only when all alerts have stopped firing for the group in prometheus will the alert be marked as resolved on pager duty.

### Example
1. Team 1 alert fired (P1-Team1-Checkout-Customer-Alert1) and an incident was created.
2. Team 2 alert fired 3 minutes after for the same grouping (P1-Team2-Checkout-Customer-Alert1) and the existing incident was updated to include the new firing alert
3. Team 2 alert was resolved 3 minutes after and the existing incident was updated to show that this alert was now resolved, but incident is still open as there are still firing alerts
4. Team 1 alert was resolved 3 minutes after and the existing incident was resolved as both alerts were now resolved
5. The team did a post mortem
![Example](example.png)


##Local usage
1. Have a local k8s cluster
2. Install [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack): `helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack -f kube/prometheus/values.yml`
3. Apply the prometheus rules and alertmanager config `kubectl apply -f kube/prometheus/alertmanagerconfig.yml kube/prometheus/prometheusrules.yml`
4. Build the image: `./gradlew bootBuildImage --imageName=nathandeamer/prometheus-alerts-to-statuspage`
5. Deploy the prometheus service and deployment `kubectl apply -f kube/service.yml kube/deployment.yml`
6. Edit the sample alert `kubectl edit prometheusrules.monitoring.coreos.com alerts` by changing to `vector(1) > 0` to make the alert fire.