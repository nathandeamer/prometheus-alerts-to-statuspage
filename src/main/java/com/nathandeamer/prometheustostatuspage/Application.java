package com.nathandeamer.prometheustostatuspage;

import com.nathandeamer.prometheustostatuspage.statuspage.configuration.HandlebarsHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(HandlebarsHints.class)
@EnableFeignClients
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
