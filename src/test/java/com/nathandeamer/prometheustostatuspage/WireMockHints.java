/*
    I've wasted a lot of time trying to get WireMock working with nativeTest - so pausing until next time

    @ImportRuntimeHints(WireMockHints.class)
    testImplementation 'org.springframework.cloud:spring-cloud-contract-wiremock:4.0.2'
    testImplementation'com.google.guava:guava:31.1-jre' // Using to register loads of wiremock tasks

    https://github.com/spring-cloud/spring-cloud-contract/blob/main/docs/src/main/asciidoc/_project-features-wiremock.adoc
    https://github.com/wiremock/wiremock/issues/1760
 */

//package com.nathandeamer.prometheustostatuspage;
//
//import com.google.common.collect.ImmutableSet;
//import com.google.common.reflect.ClassPath;
//import org.springframework.aot.hint.ExecutableMode;
//import org.springframework.aot.hint.RuntimeHints;
//import org.springframework.aot.hint.RuntimeHintsRegistrar;
//
//import java.io.IOException;
//import java.util.Arrays;
//
//public class WireMockHints implements RuntimeHintsRegistrar {
//
//    @Override
//    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
//        hints.resources().registerPattern("keystore"); // Needed for nativeTest
//        hints.resources().registerPattern("assets/*"); // Register all assets
//
//        try {
//            ImmutableSet<ClassPath.ClassInfo> allWiremockAdminClasses =  ClassPath.from(classLoader).getTopLevelClassesRecursive("com.github.tomakehurst.wiremock.admin");
//
//            // TODO: Improve this
//            for (ClassPath.ClassInfo classInfo : allWiremockAdminClasses) {
//                Class<?> clazz = classInfo.load();
//
//                if (clazz.getName().contains("Task")) {
//                    Arrays.stream(clazz.getConstructors())
//                            .forEach(r -> hints.reflection().registerConstructor(r, ExecutableMode.INVOKE));
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//}