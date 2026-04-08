package com.planno.dash_api.repository;

import com.planno.dash_api.entity.TaskCalendarLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskCalendarLinkRepository extends JpaRepository<TaskCalendarLink, Long> {

    Optional<TaskCalendarLink> findByTaskIdAndUserId(Long taskId, Long userId);

    Optional<TaskCalendarLink> findByUserIdAndCalendarIdAndExternalEventId(Long userId, String calendarId, String externalEventId);

    List<TaskCalendarLink> findAllByUserId(Long userId);

    void deleteByUserId(Long userId);
}
