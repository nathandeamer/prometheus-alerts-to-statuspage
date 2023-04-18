package com.nathandeamer.prometheustostatuspage.statuspage.configuration;

import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import lombok.SneakyThrows;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class HandlebarsHints implements RuntimeHintsRegistrar {

    @SneakyThrows
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        //hints.resources().registerPattern("helpers.nashorn.js");
        hints.reflection().registerMethod(Status.class.getMethod("name"), ExecutableMode.INVOKE);
    }
}
