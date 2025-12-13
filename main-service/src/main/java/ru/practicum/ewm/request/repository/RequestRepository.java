package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.dto.RequestStatus;
import ru.practicum.ewm.request.model.Request;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    Optional<Request> findByIdAndRequesterId(Long userId, Long requestId);

    Boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<Request> findAllByIdIn(List<Long> requestIdList);


    @Query("SELECT COUNT(r) FROM Request r WHERE r.event.id = :eventId and r.status = :status")
    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    //Long countByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    @Query("select r from Request r " +
            "join fetch r.event e " +
            "where r.requester.id = ?1 " +
            "and e.initiator.id <> ?1")
    List<Request> findAllByRequesterIdAndNotInitiator(Long userId);

    List<Request> findAllByEvent_InitiatorIdAndEvent_Id(Long userId, Long eventId);

    @Query("SELECT r.event.id AS eventId, COUNT(r) AS cnt " +
            "FROM Request r " +
            "WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    Map<Long, Long> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}