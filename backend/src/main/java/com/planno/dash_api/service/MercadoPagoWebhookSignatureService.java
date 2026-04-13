package com.planno.dash_api.service;

import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.UnauthorizedException;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MercadoPagoWebhookSignatureService {

    private static final long MAX_SIGNATURE_AGE_SECONDS = 300;

    @Value("${app.mercado-pago.webhook-secret:}")
    private String webhookSecret;

    public void validateOrThrow(@Nullable String dataId, @Nullable String requestId, @Nullable String signatureHeader) {
        if (!StringUtils.hasText(dataId)) {
            throw new BusinessException("Webhook do Mercado Pago sem data.id.");
        }
        if (!StringUtils.hasText(requestId)) {
            throw new UnauthorizedException("Webhook do Mercado Pago sem x-request-id.");
        }
        if (!StringUtils.hasText(signatureHeader)) {
            throw new UnauthorizedException("Webhook do Mercado Pago sem x-signature.");
        }
        if (!StringUtils.hasText(webhookSecret)) {
            throw new BusinessException("Mercado Pago webhook secret nao configurado.");
        }

        Map<String, String> signatureValues = Arrays.stream(signatureHeader.split(","))
                .map(String::trim)
                .map(entry -> entry.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0].trim().toLowerCase(Locale.ROOT),
                        parts -> parts[1].trim(),
                        (left, right) -> right
                ));

        String ts = signatureValues.get("ts");
        String expectedHash = signatureValues.get("v1");
        if (!StringUtils.hasText(ts) || !StringUtils.hasText(expectedHash)) {
            throw new UnauthorizedException("Webhook do Mercado Pago com assinatura invalida.");
        }
        validateTimestamp(ts);

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String computedHash = hmacSha256(manifest, webhookSecret);
        if (!MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new UnauthorizedException("Webhook do Mercado Pago com assinatura invalida.");
        }
    }

    private void validateTimestamp(String ts) {
        try {
            long timestamp = Long.parseLong(ts);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - timestamp) > MAX_SIGNATURE_AGE_SECONDS) {
                throw new UnauthorizedException("Webhook do Mercado Pago com assinatura expirada.");
            }
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("Webhook do Mercado Pago com assinatura invalida.");
        }
    }

    private String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new BusinessException("Nao foi possivel validar a assinatura do webhook do Mercado Pago.", exception);
        }
    }
}
