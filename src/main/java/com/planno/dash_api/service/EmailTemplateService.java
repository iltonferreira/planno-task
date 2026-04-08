package com.planno.dash_api.service;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailTemplateService {

    private final ResourceLoader resourceLoader;

    public EmailTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String render(String templateName, Map<String, String> parameters) {
        try (var inputStream = resourceLoader
                .getResource("classpath:templates/email/" + templateName + ".html")
                .getInputStream()) {

            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                html = html.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
            }
            return html;
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar o template de e-mail " + templateName, exception);
        }
    }
}
