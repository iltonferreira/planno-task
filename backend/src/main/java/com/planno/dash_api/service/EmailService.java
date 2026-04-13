package com.planno.dash_api.service;

import java.util.Map;

public interface EmailService {

    void sendAsync(String to, String subject, String templateName, Map<String, String> parameters);
}
