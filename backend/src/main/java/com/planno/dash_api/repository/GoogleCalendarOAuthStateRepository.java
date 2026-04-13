package com.planno.dash_api.repository;

import com.planno.dash_api.entity.GoogleCalendarOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GoogleCalendarOAuthStateRepository extends JpaRepository<GoogleCalendarOAuthState, Long> {

    Optional<GoogleCalendarOAuthState> findByState(String state);

    void deleteByUserId(Long userId);

    void deleteByState(String state);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
