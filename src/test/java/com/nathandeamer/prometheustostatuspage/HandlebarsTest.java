package com.nathandeamer.prometheustostatuspage;


import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.nathandeamer.prometheustostatuspage.alertmanager.dto.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class HandlebarsTest {

    @Autowired
    private Handlebars handlebars;

    /**
     * @see com.nathandeamer.prometheustostatuspage.statuspage.configuration.HandlebarsHints
     * There was a bug when running the native image where the Status.name method wasn't being included by the AOT,
     * which caused a condition in the template to fail.
     * @throws IOException
     */
    @Test
    public void nativeTestStatusNameMethodHasBeenIncluded() throws IOException {
        Template template = handlebars.compileInline("{{#if (eq this.name 'FIRING')}}Hello World!{{/if}}");
        String result = template.apply(Status.FIRING);
        assertThat(result).isEqualTo("Hello World!");
    }
}

