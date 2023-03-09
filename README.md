# Prometheus AlertManager to StatusPage.io
Do you have multiple alerts firing at once from Prometheus Alermanager that only effect one part of your system? And you only want to report one incident on a status page?  Then give this opinionated solution a go!

## Example:
### Scenario: Ecommerce - A customer can't check out.
This could be alerted multiple ways:
- A synthetic E2E UI checkout test could fail
- A synthetic API test could fail.
- A drop off in the number of orders being created
- A drop off in the number of baskets being created.

But the cause for all of these could be the same. e.g. A microservice required in the checkout flow has 0 instances available because of a database problem

## Usage:
### AlertManager configuration
