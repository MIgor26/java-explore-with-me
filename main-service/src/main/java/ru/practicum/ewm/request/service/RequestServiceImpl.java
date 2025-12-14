package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.NotMeetRulesException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.RequestStatus;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // Получение информации о заявках текущего пользователя на участие в чужих событиях
    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        List<Request> requestList = requestRepository.findAllByRequesterIdAndNotInitiator(userId);
        List<ParticipationRequestDto> participationRequestDtoList = new ArrayList<>();
        for (Request request : requestList) {
            participationRequestDtoList.add(requestMapper.toParticipationRequestDto(request));
        }
        log.info("Успешно получены {} заявок", requestList.size());
        return participationRequestDtoList;
    }

    // Добавление запроса от текущего пользователя на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден по id = " + userId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено по id = " + eventId));

        // Создание запроса
        Request request = new Request(LocalDateTime.now(), event, requester, RequestStatus.PENDING);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new NotMeetRulesException("Запрос уже существует: userId = {}, eventId = {} " + userId + eventId);
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new NotMeetRulesException("Инициатор не мог быть запрашивающим лицом " + userId);
        }
        if (!(event.getState().equals(EventState.PUBLISHED))) {
            throw new NotMeetRulesException("Событие еще не опубликовано");
        }

        Long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long limit = event.getParticipantLimit();

        // если есть ограничение, то проверяем. Если ограничения нет, то автоматически подтверждаем запрос
        if (limit != 0) {
            if (limit.equals(confirmedRequest)) {
                throw new NotMeetRulesException("Получено максимальное количество подтвержденных запросов: " + limit);
            }
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        // если модерация не нужна, то автоматом подтверждаем запрос и увеличиваем счетчик
        if (!event.getRequestModeration()) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        requestRepository.save(request);
        log.info("Запрос от пользователя {} на участие в событии успешно добавлен", requester);
        return requestMapper.toParticipationRequestDto(request);
    }

    // Отмена своего запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с идентификатором и/или идентификатором отправителя запроса не существует" + requestId + userId));
        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        log.info("Пользователь с id = {} успешно отменил своё участие в собитии с Id = {}", userId, requestId);
        return requestMapper.toParticipationRequestDto(request);
    }
}
