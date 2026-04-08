package com.planno.dash_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planno.dash_api.entity.Payment;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.infra.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private static final DateTimeFormatter MP_DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.mercado-pago.enabled:false}")
    private boolean enabled;

    @Value("${app.mercado-pago.access-token:}")
    private String accessToken;

    @Value("${app.mercado-pago.api-base-url:https://api.mercadopago.com}")
    private String apiBaseUrl;

    @Value("${app.mercado-pago.currency-id:BRL}")
    private String currencyId;

    @Value("${app.mercado-pago.success-url:http://localhost:4200/payments?status=success}")
    private String successUrl;

    @Value("${app.mercado-pago.pending-url:http://localhost:4200/payments?status=pending}")
    private String pendingUrl;

    @Value("${app.mercado-pago.failure-url:http://localhost:4200/payments?status=failure}")
    private String failureUrl;

    @Value("${app.mercado-pago.subscription-back-url:http://localhost:4200/subscriptions}")
    private String subscriptionBackUrl;

    @Value("${app.mercado-pago.notification-url:}")
    private String notificationUrl;

    public boolean isEnabled() {
        return enabled && StringUtils.hasText(accessToken);
    }

    public CheckoutResponse createPaymentCheckout(Payment payment, String payerEmail) {
        requireEnabled();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("external_reference", payment.getExternalReference());
        payload.put("items", List.of(Map.of(
                "id", "payment-" + payment.getId(),
                "title", payment.getTitle(),
                "description", payment.getDescription() == null ? payment.getTitle() : payment.getDescription(),
                "quantity", 1,
                "currency_id", currencyId,
                "unit_price", payment.getAmount()
        )));
        payload.put("back_urls", Map.of(
                "success", successUrl,
                "pending", pendingUrl,
                "failure", failureUrl
        ));
        payload.put("auto_return", "approved");

        if (StringUtils.hasText(notificationUrl)) {
            payload.put("notification_url", notificationUrl);
        }
        if (StringUtils.hasText(payerEmail)) {
            payload.put("payer", Map.of("email", payerEmail));
        }

        JsonNode response = executeJson("POST", "/checkout/preferences", payload);
        return new CheckoutResponse(
                response.path("id").asText(null),
                response.path("init_point").asText(null),
                response.path("sandbox_init_point").asText(null)
        );
    }

    public PaymentLinkResult createStandalonePaymentLink(
            String title,
            String description,
            BigDecimal amount,
            String payerEmail,
            String externalReference
    ) {
        requireEnabled();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("external_reference", externalReference);
        payload.put("items", List.of(Map.of(
                "id", externalReference,
                "title", title,
                "description", StringUtils.hasText(description) ? description : title,
                "quantity", 1,
                "currency_id", currencyId,
                "unit_price", amount
        )));
        payload.put("back_urls", Map.of(
                "success", successUrl,
                "pending", pendingUrl,
                "failure", failureUrl
        ));
        payload.put("auto_return", "approved");

        if (StringUtils.hasText(payerEmail)) {
            payload.put("payer", Map.of("email", payerEmail));
        }

        JsonNode response = executeJson("POST", "/checkout/preferences", payload);
        return new PaymentLinkResult(
                externalReference,
                response.path("id").asText(null),
                response.path("init_point").asText(null),
                response.path("sandbox_init_point").asText(null)
        );
    }

    public SubscriptionCheckoutResult createSubscriptionCheckout(Subscription subscription, String payerEmail) {
        return createHostedSubscriptionCheckout(
                subscription.getDescription(),
                subscription.getExternalReference(),
                payerEmail,
                subscription.getPrice(),
                subscriptionBackUrl,
                notificationUrl
        );
    }

    public SubscriptionCheckoutResult createHostedSubscriptionCheckout(
            String reason,
            String externalReference,
            String payerEmail,
            BigDecimal amount,
            String backUrl,
            String webhookUrl
    ) {
        requireEnabled();

        if (!StringUtils.hasText(payerEmail)) {
            throw new BusinessException("A assinatura precisa de um email valido para gerar checkout recorrente.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", reason);
        payload.put("external_reference", externalReference);
        payload.put("payer_email", payerEmail);
        payload.put("back_url", backUrl);
        payload.put("status", "pending");
        payload.put("auto_recurring", Map.of(
                "frequency", 1,
                "frequency_type", "months",
                "transaction_amount", amount,
                "currency_id", currencyId,
                "start_date", LocalDateTime.now().plusMinutes(5).atOffset(ZoneOffset.UTC).format(MP_DATE_FORMATTER)
        ));

        if (StringUtils.hasText(webhookUrl)) {
            payload.put("notification_url", webhookUrl);
        }

        JsonNode response = executeJson("POST", "/preapproval", payload);
        return new SubscriptionCheckoutResult(
                response.path("id").asText(null),
                response.path("init_point").asText(null),
                response.path("status").asText(null)
        );
    }

    public RemotePayment fetchPayment(String externalPaymentId) {
        requireEnabled();
        JsonNode response = executeJson("GET", "/v1/payments/" + urlEncode(externalPaymentId), null);
        return new RemotePayment(
                response.path("id").asText(null),
                response.path("status").asText(null),
                response.path("status_detail").asText(null),
                response.path("external_reference").asText(null),
                response.path("description").asText(null),
                response.path("transaction_amount").decimalValue(),
                parseDateTime(response.path("date_approved").asText(null))
        );
    }

    public RemoteSubscription fetchSubscription(String externalSubscriptionId) {
        requireEnabled();
        JsonNode response = executeJson("GET", "/preapproval/" + urlEncode(externalSubscriptionId), null);
        JsonNode autoRecurring = response.path("auto_recurring");

        return new RemoteSubscription(
                response.path("id").asText(null),
                response.path("status").asText(null),
                response.path("external_reference").asText(null),
                response.path("reason").asText(null),
                response.path("init_point").asText(null),
                response.path("payer_email").asText(null),
                autoRecurring.path("transaction_amount").decimalValue(),
                autoRecurring.path("currency_id").asText(currencyId),
                parseDate(response.path("next_payment_date").asText(null))
        );
    }

    public List<RemoteSubscriptionSummary> searchSubscriptions() {
        requireEnabled();
        List<RemoteSubscriptionSummary> subscriptions = new ArrayList<>();
        int limit = 100;
        int offset = 0;
        int total = Integer.MAX_VALUE;

        while (offset < total) {
            JsonNode response = executeJson("GET", "/preapproval/search?limit=" + limit + "&offset=" + offset, null);
            JsonNode results = response.path("results");

            if (!results.isArray() || results.isEmpty()) {
                break;
            }

            for (JsonNode item : results) {
                JsonNode autoRecurring = item.path("auto_recurring");
                subscriptions.add(new RemoteSubscriptionSummary(
                        item.path("id").asText(null),
                        item.path("reason").asText(null),
                        item.path("status").asText(null),
                        item.path("payer_email").asText(null),
                        item.path("external_reference").asText(null),
                        item.path("init_point").asText(null),
                        autoRecurring.path("transaction_amount").decimalValue(),
                        autoRecurring.path("currency_id").asText(currencyId),
                        parseDate(item.path("next_payment_date").asText(null))
                ));
            }

            int pageCount = results.size();
            total = response.path("paging").path("total").asInt(offset + pageCount);
            offset += pageCount;

            if (pageCount < limit) {
                break;
            }
        }

        return subscriptions;
    }

    private JsonNode executeJson(String method, String path, Object payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json");

            if ("GET".equals(method)) {
                builder.GET();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Mercado Pago respondeu com erro: " + response.body());
            }

            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao comunicar com o Mercado Pago.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao comunicar com o Mercado Pago.", exception);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return OffsetDateTime.parse(value, MP_DATE_FORMATTER).toLocalDateTime();
    }

    private LocalDate parseDate(String value) {
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new BusinessException("Mercado Pago nao configurado.");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record CheckoutResponse(
            String preferenceId,
            String initPoint,
            String sandboxInitPoint
    ) {
    }

    public record PaymentLinkResult(
            String externalReference,
            String preferenceId,
            String initPoint,
            String sandboxInitPoint
    ) {
    }

    public record SubscriptionCheckoutResult(
            String subscriptionId,
            String initPoint,
            String status
    ) {
    }

    public record RemotePayment(
            String id,
            String status,
            String statusDetail,
            String externalReference,
            String description,
            BigDecimal amount,
            LocalDateTime approvedAt
    ) {
    }

    public record RemoteSubscription(
            String id,
            String status,
            String externalReference,
            String reason,
            String initPoint,
            String payerEmail,
            BigDecimal amount,
            String currencyId,
            LocalDate nextPaymentDate
    ) {
    }

    public record RemoteSubscriptionSummary(
            String id,
            String reason,
            String status,
            String payerEmail,
            String externalReference,
            String initPoint,
            BigDecimal amount,
            String currencyId,
            LocalDate nextPaymentDate
    ) {
    }
}
