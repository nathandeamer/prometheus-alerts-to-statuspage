package com.nathandeamer.prometheustostatuspage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.concurrent.Future;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.LazyFuture;

// TODO:
// 1. Start up wiremock container WITH some test endpoints.
// 2. Sourcesets?

public class ApplicationComponentTest {

    private static final Future<String> IMAGE_FUTURE = new LazyFuture<>() {
        @Override
        protected String resolve() {
            // Find project's root dir
            File cwd;
            cwd = new File(".");
            while (!new File(cwd, "settings.gradle").isFile()) {
                cwd = cwd.getParentFile();
            }

            var imageName = String.format(
                    "prometheus-alerts-to-statuspage:%s",
                    System.currentTimeMillis()
            );

            // Run Gradle task and override the image name
            GradleRunner.create()
                    .withProjectDir(cwd)
                    //.withArguments( "-x","test","-q", "bootBuildImage", "--imageName", imageName) // -x (excluse test), -q (logging level quiet)
                    .withArguments("bootBuildImage", "--imageName", imageName)
                    .forwardOutput()
                    .build();

            return imageName;
        }
    };

    @Container
    static final GenericContainer<?> underTest = new GenericContainer<>(IMAGE_FUTURE)
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/actuator/health") // Override the default wait strategy for distroless images. https://www.testcontainers.org/features/startup_and_waits/ - See: https://github.com/testcontainers/testcontainers-java/issues/3835
                    .forStatusCode(200));

    WebTestClient webClient;

    @BeforeEach
    void setUp() {
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
        var endpoint = String.format(
                "http://%s:%d/",
                underTest.getHost(),
                underTest.getFirstMappedPort()
        );

        // Option 1:
        TestRestTemplate testRestTemplate = new TestRestTemplate();
        ResponseEntity<String> response = testRestTemplate.
                getForEntity(endpoint + "/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Option 2:
        webClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }

}
