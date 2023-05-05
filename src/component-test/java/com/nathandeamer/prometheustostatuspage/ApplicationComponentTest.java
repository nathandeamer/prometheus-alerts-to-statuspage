package com.nathandeamer.prometheustostatuspage;

import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Alert;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.AlertWrapper;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ComponentStatus;
import com.nathandeamer.prometheustostatuspage.statuspage.dto.ImpactOverride;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.LazyFuture;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.STATUSPAGE_SUMMARY_VALUE;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.buildAlert;
import static com.nathandeamer.prometheustostatuspage.AlertWrapperHelper.buildAlertWrapper;
import static org.testcontainers.containers.Network.newNetwork;

/*
    2023-05-05: I'm STUCK.
    Spring Boot Native cannot dynamically replace properties with environment variables supplied at runtime (I don't think).
    The values needs to be backed in at
    No this can't be true, as it is able to
 */

// TODO:
// 1. Start up wiremock container WITH some test endpoints
// 2. How do I tell my built image
public class ApplicationComponentTest {

    private static final Future<String> IMAGE_FUTURE =
            new LazyFuture<>() {
                @Override
                protected String resolve() {
                    // Find project's root dir
                    File cwd;
                    cwd = new File(".");
                    while (!new File(cwd, "settings.gradle").isFile()) {
                        cwd = cwd.getParentFile();
                    }

                    var imageName =
                            String.format("prometheus-alerts-to-statuspage:%s", System.currentTimeMillis());

                    // Run Gradle task and override the image name
                    GradleRunner.create()
                            .withProjectDir(cwd)
                            .withArguments( "-x","test","-q", "bootBuildImage", "--imageName", imageName)
                            .forwardOutput()
                            .build();

                    return imageName;
                }
            };

//    @Container
//    public GenericContainer<?> underTest =
//            new GenericContainer<>(IMAGE_FUTURE)
//                    .withExposedPorts(8080)
//                    .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(ApplicationComponentTest.class)))
//                    .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200)); // Override the default wait strategy for distroless images. https://www.testcontainers.org/features/startup_and_waits/ - See: https://github.com/testcontainers/testcontainers-java/issues/3835

    Network network = newNetwork();

    @Container
    public GenericContainer<?> underTest = new GenericContainer<>("library/prometheus-alerts-to-statuspage:0.0.1-SNAPSHOT")
            .withExposedPorts(8080)
            .withEnv("STATUSPAGE_APIURL", "http://wiremock:8080")
            .withNetwork(network)
            .withNetworkAliases("prometheus-alerts-to-statuspage")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

    @Container
    public WireMockContainer wiremockServer =
            new WireMockContainer("2.35.0")
                    .withNetwork(network)
                    .withNetworkAliases("wiremock")
                    .withMapping("unresolved", WiremockTestContainerTest.class, "statuspage-unresolved.json")
                    .withMapping("hello", WiremockTestContainerTest.class, "hello-world.json");

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        wiremockServer.start();
        underTest.start();

        var endpoint = String.format(
                "http://%s:%d/",
                underTest.getHost(),
                underTest.getFirstMappedPort()
        );
        webClient = WebTestClient.bindToServer().baseUrl(endpoint).build();
    }

    @Test
    public void healthy() {
        webClient.get().uri("/actuator/health").exchange().expectStatus().is2xxSuccessful();
    }

    // TODO: Wiremock: /pages/{pageId}/incidents/unresolved"
    @Test
    public void trySomethingReal() throws Exception {
        List<Alert> alerts = List.of(buildAlert(Status.FIRING, ImpactOverride.MINOR, com.nathandeamer.prometheustostatuspage.statuspage.dto.Status.INVESTIGATING, ComponentStatus.MAJOR_OUTAGE, STATUSPAGE_SUMMARY_VALUE));
        AlertWrapper alertWrapper = buildAlertWrapper(Status.FIRING, alerts);

        String url = String.format("http://%s:%d/alert", underTest.getHost(), underTest.getFirstMappedPort());

        System.out.println("URL: " + url);


        EntityExchangeResult<String> result = webClient.post().uri("/alert").contentType(MediaType.APPLICATION_JSON).bodyValue(alertWrapper).exchange().expectBody(String.class).returnResult();

        System.out.println(result.getResponseBody());


        System.out.println(underTest.getLogs());

    }

}
