package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.StatsClient;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.LocationRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.NotMeetRulesException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.dto.*;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private String appName = "main-service";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final RequestMapper requestMapper;

    // Админ: поиск событий
    @Override
    public List<EventFullDto> getEventsByAdmin(AdminEventFilter filter) {
        List<Event> events = handleFilters(filter, filter.getFrom(), filter.getSize());
        log.info("По указанным фильтрам найдено {} событий", events.size());
        return events.stream()
                .map(eventMapper::toEventFullDto)
                .map(this::addConfirmedRequestsAndViews)
                .toList();
    }

    // Админ: редактирование данных события и его статуса (отклонение/публикация).
    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (updateEventAdminRequest.getEventDate() != null) {
            if (updateEventAdminRequest.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
                throw new ValidationException("дата начала изменяемого события должна быть не ранее " +
                        "чем за час от даты публикации.");
            }
        }
        if (updateEventAdminRequest.getStateAction() != null) {
            if (updateEventAdminRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (eventToUpdate.getState().equals(EventState.PENDING)) {
                    eventToUpdate.setState(EventState.PUBLISHED);
                    eventToUpdate.setPublishedOn(LocalDateTime.now());
                } else {
                    throw new NotMeetRulesException("Событие можно публиковать,только если оно в состоянии " +
                            "ожидания публикации");
                }
            }
            if (updateEventAdminRequest.getStateAction() == StateActionAdmin.REJECT_EVENT) {
                if (eventToUpdate.getState().equals(EventState.PUBLISHED)) {
                    throw new NotMeetRulesException("Событие можно отклонить, только если оно еще не опубликовано.");
                }
                eventToUpdate.setState(EventState.CANCELED);
            }
        }
        updateEventEntity(updateEventAdminRequest, eventToUpdate);
        eventRepository.save(eventToUpdate);
        log.info("Событие успешно обновлено.");
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToUpdate));
    }

    // Приватные: получение событий, добавленных текущим пользователем
    @Override
    public List<EventShortDto> getEventsByInitiator(Long userId, Pageable pageable) {
        // Получение всех событий инициатора
        List<Event> eventList = eventRepository.findAllByInitiatorId(userId, pageable).toList();
        if (eventList.isEmpty()) {
            return Collections.emptyList();
        }
        List<EventShortDto> eventShortDtoList = eventList.stream()
                .map(eventMapper::toEventShortDto)
                .toList();
        // Получение списка id событий
        List<Long> eventIds = eventShortDtoList.stream()
                .map(EventShortDto::getId)
                .toList();

        // Получение одобренных заявок одним запросом
        Map<Long, Long> confirmedCounts = requestRepository.countConfirmedByEventIds(eventIds);

        // Получение просмотров одним запросом к statsClient
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();
        List<ViewStatsDto> viewStatsDtoList = statsClient.getStats(LocalDateTime.now().minusYears(100),
                LocalDateTime.now(), uris, true);
        Map<Long, Long> viewsMap = viewStatsDtoList.stream()
                .collect(Collectors.toMap(
                        v -> Long.parseLong(v.getUri().split("/")[2]),
                        ViewStatsDto::getHits
                ));

        // Окончательное формирование списка событий
        eventShortDtoList.forEach(e -> {
            e.setConfirmedRequests(confirmedCounts.getOrDefault(e.getId(), 0L));
            e.setViews(viewsMap.getOrDefault(e.getId(), 0L));
        });
        log.info("Получено {} событий, добавленных текущим пользователем ", eventShortDtoList.size());
        return eventShortDtoList;
    }

    // Приватные: добавление нового события
    @Override
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));

        if (newEventDto.getEventDate().minusHours(2).isBefore(LocalDateTime.now())) {
            throw new ValidationException("Дата и время на которые намечено событие не может быть раньше," +
                    "чем через два часа от текущего момента");
        }

        // Формирование события для сохранения в БД
        Event eventToSave = eventMapper.toEvent(newEventDto);
        eventToSave.setState(EventState.PENDING);
        eventToSave.setCreatedOn(LocalDateTime.now());
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));
        eventToSave.setCategory(category);
        eventToSave.setInitiator(user);
        eventRepository.save(eventToSave);
        log.info("НОВОЕ. ДО СОБЫТИЯ");
        System.out.println("НОВОЕ ДО СОБЫТИЯ СОУТ");
        log.info("Событие успешно сохранено");
        System.out.println("Добавил для теста возможность просмотра Соут");
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToSave));
    }

    // Приватные: получение полной информации о событии добавленном текущим пользователем
    @Override
    public EventFullDto getEventByInitiator(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(event));
    }

    // Приватные: изменение события добавленного текущим пользователем
    @Override
    public EventFullDto updateEventByInitiator(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {

        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (eventToUpdate.getState().equals(EventState.CANCELED) || eventToUpdate.getState().equals(EventState.PENDING)) {
            if (updateEventUserRequest.getEventDate() != null
                    && updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, " +
                        "чем через два часа от текущего момента");
            }
            if (StateActionUser.SEND_TO_REVIEW == updateEventUserRequest.getStateAction()) {
                eventToUpdate.setState(EventState.PENDING);
            }
            if (StateActionUser.CANCEL_REVIEW == updateEventUserRequest.getStateAction()) {
                eventToUpdate.setState(EventState.CANCELED);
            }
        } else {
            throw new NotMeetRulesException("Изменить можно только отмененные события " +
                    "или события в состоянии ожидания модерации");
        }

        updateEventEntity(updateEventUserRequest, eventToUpdate);
        eventRepository.save(eventToUpdate);
        log.info("Событие успешно изменено");
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToUpdate));
    }

    // Приватные: Получение инфо о запросах на участие в событии текущего пользователя
    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUserOfCurrentEvent(Long userId, Long eventId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не Найден по id = " + userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено по id = " + eventId));

        // Является ли user инициатором
        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new ValidationException("Пользователь " + userId + " не является инициатором события " + eventId);
        }

        List<Request> requestList = requestRepository
                .findAllByEvent_InitiatorIdAndEvent_Id(userId, eventId);
        log.info("Информация о событиях пользователя по id = {} успешно получена", userId);
        return requestList.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }


    // Приватные: изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя
    @Override
    public EventRequestStatusUpdateResult updateRequest(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest eventRequest) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден по id = " + userId);
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не Найдено по id = " + eventId));

        if (event.getParticipantLimit() == 0 && !event.getRequestModeration()) {
            throw new ValidationException("Модерация не требуется " + eventId);
        }

        Long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (confirmedRequest >= event.getParticipantLimit()) {
            throw new NotMeetRulesException("Превышение лимита участия " + eventId);
        }

        // Список всех запросов статус которых нужно обновить
        List<Long> requestIdList = eventRequest.getRequestIds();
        // получаем статус события, который нужно проставить у всех событий
        RequestStatusUpdate status = eventRequest.getStatus();

        List<Request> requestList = requestRepository.findAllByIdIn(requestIdList);
        if (requestList.isEmpty()) {
            throw new NotFoundException("Запросов не существует ");
        }

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        List<Request> updatedRequests = new ArrayList<>();

        // перебираем все запросы
        for (Request currentRequest : requestList) {
            if (status == RequestStatusUpdate.CONFIRMED && currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                if (currentRequest.getStatus().equals(RequestStatus.CONFIRMED)) {
                    throw new NotMeetRulesException("Запрос уже был подтвержден");
                }
                if (confirmedRequest >= event.getParticipantLimit()) {
                    // всем отказываем когда превышен лимит
                    currentRequest.setStatus(RequestStatus.REJECTED);
                    updatedRequests.add(currentRequest);
                    rejectedRequests.add(currentRequest);
                }
                currentRequest.setStatus(RequestStatus.CONFIRMED);
                updatedRequests.add(currentRequest);
                confirmedRequest++;
                confirmedRequests.add(currentRequest);
            }
            if (status == RequestStatusUpdate.REJECTED && currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                // отказываем когда событие отменилось
                currentRequest.setStatus(RequestStatus.REJECTED);
                updatedRequests.add(currentRequest);
                rejectedRequests.add(currentRequest);
            }
        }

        // сохранили все запросы с новыми статусами в БД
        requestRepository.saveAll(updatedRequests);
        eventRepository.save(event);

        // переводим в ДТО и на выход
        List<ParticipationRequestDto> confirmedRequestsDto =
                confirmedRequests.stream().map(requestMapper::toParticipationRequestDto).collect(Collectors.toList());
        List<ParticipationRequestDto> rejectedRequestsDto =
                rejectedRequests.stream().map(requestMapper::toParticipationRequestDto).collect(Collectors.toList());

        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        updateResult.setConfirmedRequests(confirmedRequestsDto);
        updateResult.setRejectedRequests(rejectedRequestsDto);
        log.info("Данные изменены успешно");
        return updateResult;
    }

    // Публичные: получение событий с возможностью фильтрации
    @Override
    public List<EventShortDto> getEventList(PublicEventFilter filter) {

        String text = filter.getText();
        List<Long> categories = filter.getCategories();
        Boolean paid = filter.getPaid();
        String rangeStart = filter.getRangeStart();
        String rangeEnd = filter.getRangeEnd();
        Boolean onlyAvailable = filter.getOnlyAvailable() == null ? false : filter.getOnlyAvailable();
        String sort = filter.getSort();
        Integer from = filter.getFrom() == null ? 0 : filter.getFrom();
        Integer size = filter.getSize() == null ? 10 : filter.getSize();
        String userIp = filter.getUserIp();
        String requestUri = filter.getRequestUri();

        LocalDateTime start;
        LocalDateTime end;
        if (rangeStart != null && rangeEnd != null) {
            start = LocalDateTime.parse(rangeStart, DTF);
            end = LocalDateTime.parse(rangeEnd, DTF);
            if (start.isAfter(end)) {
                throw new ValidationException("Дата старта не может быть позже даты окончания.");
            }
        }
        start = rangeStart == null ? LocalDateTime.now() : LocalDateTime.parse(rangeStart, DTF);
        end = rangeEnd != null ? LocalDateTime.parse(filter.getRangeEnd(), DTF) : null;

        Pageable pageable;
        if (sort == null) {
            pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        } else if (sort.equalsIgnoreCase("VIEWS")) {
            pageable = PageRequest.of(from / size, size, Sort.by("views").ascending());
        } else if (sort.equalsIgnoreCase("EVENT_DATE")) {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        } else {
            throw new ValidationException("Указан некорректный вариант сортировки");
        }

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(appName)
                .uri(requestUri)
                .ip(userIp)
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.addHit(hitDto);

        List<Event> events = eventRepository.findEvents(
                text == null ? null : text.toLowerCase(),
                categories,
                paid,
                start,
                end,
                onlyAvailable,
                EventState.PUBLISHED,
                pageable
        );

        List<EventShortDto> result = events.stream()
                .map(eventMapper::toEventShortDto)
                .map(this::addShortConfirmedRequestsAndViews)
                .collect(Collectors.toList());
        log.info("Получено {} событий", result.size());
        return result;
    }

    // Публичные: получение подробной информации об опубликованном событии по его идентификатору
    @Override
    public EventFullDto getEvent(Long eventId, String userIp, String requestUri) {

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено."));

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(appName)
                .uri(requestUri)
                .ip(userIp)
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.addHit(hitDto);
        log.info("Событие получено");
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(event));
    }


    // ---------------------------------------- ПРИВАТНЫЕ МЕТОДЫ --------------------------------------------------- \\

    private List<Event> handleFilters(AdminEventFilter filter, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        // Простой запрос в случае отсутствия фильтров
        boolean noFilters =
                filter.getUsers() == null &&
                        filter.getStates() == null &&
                        filter.getCategories() == null &&
                        filter.getRangeStart() == null &&
                        filter.getRangeEnd() == null;
        if (noFilters) {
            return eventRepository.findAll(pageRequest).toList();
        }
        // Формирование параметров запроса
        List<EventState> states = null;
        if (filter.getStates() != null) {
            states = filter.getStates().stream()
                    .map(EventState::valueOf)
                    .toList();
        }
        List<Long> users = filter.getUsers();
        List<Long> categories = filter.getCategories();
        LocalDateTime start = filter.getRangeStart() != null ? LocalDateTime.parse(filter.getRangeStart(), DTF) : null;
        LocalDateTime end = filter.getRangeEnd() != null ? LocalDateTime.parse(filter.getRangeEnd(), DTF) : null;
        // Запрос с фильтрами
        return eventRepository.findAdminEvents(
                users,
                states,
                categories,
                start,
                end,
                pageRequest
        );
    }


    private EventFullDto addConfirmedRequestsAndViews(EventFullDto eventFullDto) {
        log.info("Начало работы addConfirmedRequestsAndViews");
        System.out.println("Начало работы addConfirmedRequestsAndViews");

        // Подтверждённые запросы
        eventFullDto.setConfirmedRequests(
                requestRepository.countByEventIdAndStatus(eventFullDto.getId(), RequestStatus.CONFIRMED));
        System.out.println("Добавлены подтверждённые запросы = " + eventFullDto.getConfirmedRequests());

        // Получение просмотров
        LocalDateTime start = eventFullDto.getPublishedOn() != null
                ? eventFullDto.getPublishedOn()
                : LocalDateTime.now().minusYears(100);
        List<String> uris = List.of("/events/" + eventFullDto.getId());
        System.out.println("Сформированы параметры для statsClient");
        System.out.println("start = " + start);
        System.out.println("LocalDateTime.now() = " + LocalDateTime.now());
        System.out.println("uris = " + uris);
        List<ViewStatsDto> viewStatsDtoList = statsClient.getStats(start, LocalDateTime.now(), uris, true);
//        Map<Long, Long> viewsMap = viewStatsDtoList.stream()
//                .collect(Collectors.toMap(
//                        v -> Long.parseLong(v.getUri().split("/")[2]),
//                        ViewStatsDto::getHits
//                ));
        eventFullDto.setViews(viewStatsDtoList.isEmpty()
                ? 0L
                : viewStatsDtoList.getFirst().getHits());

        return eventFullDto;
    }


    private EventShortDto addShortConfirmedRequestsAndViews(EventShortDto eventShortDto) {

        eventShortDto.setConfirmedRequests(
                requestRepository.countByEventIdAndStatus(eventShortDto.getId(), RequestStatus.CONFIRMED));

        // Добавить views к каждому событию
        List<String> uris = new ArrayList<>();

        // создаем uri для обращения к базе данных статистики
        uris.add("/events/" + eventShortDto.getId());

        List<ViewStatsDto> viewStatsDtoList = statsClient.getStats(LocalDateTime.now().minusYears(100), LocalDateTime.now(), uris, true);
        if (viewStatsDtoList.isEmpty()) {
            eventShortDto.setViews(0L);
        } else {
            eventShortDto.setViews(viewStatsDtoList.getFirst().getHits());
        }
        return eventShortDto;
    }

    private void updateEventEntity(UpdateEventAdminRequest event, Event eventToUpdate) {
        eventToUpdate.setAnnotation(Objects.requireNonNullElse(event.getAnnotation(), eventToUpdate.getAnnotation()));
        eventToUpdate.setCategory(event.getCategory() == null
                ? eventToUpdate.getCategory()
                : categoryRepository.findById(event.getCategory()).orElseThrow(() -> new NotFoundException("Категория не найдена")));
        eventToUpdate.setDescription(Objects.requireNonNullElse(event.getDescription(), eventToUpdate.getDescription()));
        eventToUpdate.setEventDate(Objects.requireNonNullElse(event.getEventDate(), eventToUpdate.getEventDate()));
        eventToUpdate.setLocation(event.getLocation() == null
                ? eventToUpdate.getLocation()
                : locationRepository.findByLatAndLon(event.getLocation().getLat(), event.getLocation().getLon())
                .orElse(new Location(null, event.getLocation().getLat(), event.getLocation().getLon())));
        eventToUpdate.setPaid(Objects.requireNonNullElse(event.getPaid(), eventToUpdate.getPaid()));
        eventToUpdate.setParticipantLimit(Objects.requireNonNullElse(event.getParticipantLimit(), eventToUpdate.getParticipantLimit()));
        eventToUpdate.setRequestModeration(Objects.requireNonNullElse(event.getRequestModeration(), eventToUpdate.getRequestModeration()));
        eventToUpdate.setTitle(Objects.requireNonNullElse(event.getTitle(), eventToUpdate.getTitle()));
    }

    private void updateEventEntity(UpdateEventUserRequest event, Event eventToUpdate) {
        eventToUpdate.setAnnotation(Objects.requireNonNullElse(event.getAnnotation(), eventToUpdate.getAnnotation()));
        eventToUpdate.setCategory(event.getCategory() == null
                ? eventToUpdate.getCategory()
                : categoryRepository.findById(event.getCategory()).orElseThrow(() -> new NotFoundException("Категория не найдена")));
        eventToUpdate.setDescription(Objects.requireNonNullElse(event.getDescription(), eventToUpdate.getDescription()));
        eventToUpdate.setEventDate(Objects.requireNonNullElse(event.getEventDate(), eventToUpdate.getEventDate()));
        eventToUpdate.setLocation(event.getLocation() == null
                ? eventToUpdate.getLocation()
                : locationRepository.findByLatAndLon(event.getLocation().getLat(), event.getLocation().getLon())
                .orElse(new Location(null, event.getLocation().getLat(), event.getLocation().getLon())));
        eventToUpdate.setPaid(Objects.requireNonNullElse(event.getPaid(), eventToUpdate.getPaid()));
        eventToUpdate.setParticipantLimit(Objects.requireNonNullElse(event.getParticipantLimit(), eventToUpdate.getParticipantLimit()));
        eventToUpdate.setRequestModeration(Objects.requireNonNullElse(event.getRequestModeration(), eventToUpdate.getRequestModeration()));
        eventToUpdate.setTitle(Objects.requireNonNullElse(event.getTitle(), eventToUpdate.getTitle()));
    }
}