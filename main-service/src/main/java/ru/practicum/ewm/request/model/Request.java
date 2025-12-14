package ru.practicum.ewm.request.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.dto.RequestStatus;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created")
    private LocalDateTime created; // Дата и время создания заявки

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event; // Идентификатор события

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester; // Идентификатор пользователя, отправившего заявку

    @Enumerated(EnumType.STRING)
    private RequestStatus status; // Статус заявки

    public Request(LocalDateTime created, Event event, User requester, RequestStatus status) {
        this.created = created;
        this.event = event;
        this.requester = requester;
        this.status = status;
    }
}
