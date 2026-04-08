package com.planno.dash_api.repository;

import com.planno.dash_api.entity.GoogleCalendarConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, Long> {

    Optional<GoogleCalendarConnection> findByUserId(Long userId);
}
