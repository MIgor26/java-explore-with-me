package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT e
              FROM Event e
             WHERE (:users IS NULL OR e.initiator.id IN :users)
               AND (:states IS NULL OR e.state IN :states)
               AND (:categories IS NULL OR e.category.id IN :categories)
               AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
               AND e.eventDate <= COALESCE(:rangeEnd,   e.eventDate)
             ORDER BY e.id
            """)
    List<Event> findAdminEvents(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    @Query("""
            SELECT e
              FROM Event e
             WHERE e.state = :state
               AND (:text IS NULL
                    OR (LOWER(e.annotation) LIKE %:text%
                        OR LOWER(e.description) LIKE %:text%
                    )
               )
               AND (:categories IS NULL OR e.category.id IN :categories)
               AND (:paid IS NULL OR e.paid = :paid)
               AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
               AND e.eventDate <= COALESCE(:rangeEnd,   e.eventDate)
               AND (:onlyAvailable IS NULL
                    OR :onlyAvailable = false
                    OR COALESCE(e.participantLimit, 0) = 0
               )
            """)
    List<Event> findEvents(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("state") EventState state,
            Pageable pageable
    );

    Optional<Event> findByIdAndState(Long eventId, EventState eventStatus);

    List<Event> findAllByCategoryId(Long categoryId);

    Set<Event> findAllByIdIn(Set<Long> eventIdList);
}
