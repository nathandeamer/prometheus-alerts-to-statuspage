package com.nathandeamer.prometheustostatuspage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.wiremock.integrations.testcontainers.WireMockContainer;

public class WiremockTestContainerTest {

    public static final String HOST_PORT = "49162";
    @Container
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withMapping("hello", WiremockTestContainerTest.class, "hello-world.json");

    @BeforeEach
    void setUp() {
        // https://www.testcontainers.org/features/networking/#:~:text=From%20the%20host%27s%20perspective%20Testcontainers%20actually%20exposes%20this%20on%20a%20random%20free%20port.%20This%20is%20by%20design%2C%20to%20avoid%20port%20collisions%20that%20may%20arise%20with%20locally%20running%20software%20or%20in%20between%20parallel%20test%20runs.
        // https://github.com/testcontainers/testcontainers-java/issues/256
        wiremockServer.getPortBindings().add(HOST_PORT + ":8080"); // Hardcode the port being exposed to the host machine (Not recommended!)
        wiremockServer.start();
    }

    @Test
    public void helloWorld() throws Exception {
        System.out.println(wiremockServer.getRequestURI("hello"));
        System.out.println(wiremockServer.getServerPort());
        System.out.println(wiremockServer.getFirstMappedPort());

        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + HOST_PORT + "/hello")) //.uri(wiremockServer.getRequestURI("hello"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body())
                .as("Wrong response body")
                .contains("Hello, world!");
    }

}
