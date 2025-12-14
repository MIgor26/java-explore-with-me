package ru.practicum.ewm.event.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "annotation", nullable = false)
    String annotation;

    @OneToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    Category category; // категории к которой относится событие

    @Column(name = "created_on")
    LocalDateTime createdOn; // Дата и время создания события (в формате "yyyy-MM-dd HH:mm:ss")

    @Column(name = "description", nullable = false)
    String description; // Полное описание события

    @Column(name = "event_date")
    LocalDateTime eventDate; // Дата и время на которые намечено событие (в формате "yyyy-MM-dd HH:mm:ss")

    @OneToOne
    @JoinColumn(name = "initiator_id", referencedColumnName = "id")
    User initiator; // Пользователь (краткая информация)

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "location_id", referencedColumnName = "id")
    Location location; // Широта и долгота места проведения события

    @Column(name = "paid")
    Boolean paid; // Нужно ли оплачивать участие

    @Column(name = "participation_limit")
    Long participantLimit; // Ограничение на количество участников. Значение 0 - означает отсутствие ограничения

    @Column(name = "published_on")
    LocalDateTime publishedOn; // Дата и время публикации события (в формате "yyyy-MM-dd HH:mm:ss")

    @Column(name = "request_moderation")
    Boolean requestModeration; // Нужна ли пре-модерация заявок на участие

    @Enumerated(EnumType.STRING)
    EventState state; // Список состояний жизненного цикла события

    @Column(name = "title")
    String title; // Заголовок
}
