package com.planno.dash_api.infra;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.planno.dash_api.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.issuer:planno-tasks-api}")
    private String issuer;

    @Value("${api.security.token.expiration-hours:2}")
    private long expirationHours;

    @Value("${app.security.production:false}")
    private boolean production;

    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(secret) || secret.length() < MIN_SECRET_LENGTH) {
            if (production) {
                throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres em producao.");
            }

            secret = "dev-only-planno-tasks-secret-change-before-prod";
            LOGGER.warn("JWT_SECRET ausente ou fraco. Usando segredo local de desenvolvimento; defina JWT_SECRET forte em producao.");
        }
    }

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(user.getEmail())
                    .withClaim("tenantId", user.getTenant().getId())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(Math.max(1, expirationHours)).toInstant(ZoneOffset.of("-03:00"));
    }
}
