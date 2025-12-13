package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.StatsClient;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;

    // Администрирование: добавление новой подборки (подборка может не содержать событий)
    @Override
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {

        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Long> eventIdList = newCompilationDto.getEvents();
            Set<Event> events = eventRepository.findAllByIdIn(eventIdList);
            compilation.setEvents(events);
        } else {
            compilation.setEvents(Collections.emptySet());
        }
        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка = {} успешно сохранена", saved.getId());
        return addConfirmedRequestsAndViews(compilationMapper.toCompilationDto(saved));
    }

    // Администрирование: удаление подборки
    @Override
    public void deleteCompilation(Long compilationId) {
        compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Подборка не найдена с id = " + compilationId));
        compilationRepository.deleteById(compilationId);
    }

    // Администрирование: обновить информацию о подборке
    @Override
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateCompilationRequest) {

        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Подборка не найдена по id = " + compilationId));

        // если в присланной подборке есть события, то сохраняем их в выгруженной подборке
        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            Set<Long> eventIdList = updateCompilationRequest.getEvents();
            Set<Event> events = eventRepository.findAllByIdIn(eventIdList);
            compilation.setEvents(events);
        }
        // обновляем закреплено на главной странице или нет
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }
        // обновляем название/заголовок
        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }
        compilationRepository.save(compilation);
        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
        log.info("Подборка успешно обновлена");
        return addConfirmedRequestsAndViews(compilationDto);
    }

    // Публичное: получение подборок событий
    @Override
    public List<CompilationDto> getCompilationList(Boolean pinned, Integer from, Integer size) {

        int safeFrom = (from != null) ? from : 0;
        int safeSize = (size != null && size > 0) ? size : 10;

        Pageable pageable = PageRequest.of(safeFrom / safeSize, safeSize);

        Page<Compilation> compilationPage =
                (pinned != null)
                        ? compilationRepository.findAllByPinnedOrderByIdDesc(pinned, pageable)
                        : compilationRepository.findAll(pageable);

        List<Compilation> compilationList = compilationPage.getContent();

        if (compilationList.isEmpty()) return List.of();

        // Собираем все eventIds
        List<Long> eventIds = compilationList.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(Event::getId)
                .distinct()
                .toList();

        if (eventIds.isEmpty()) {
            return compilationList.stream()
                    .map(compilationMapper::toCompilationDto)
                    .toList();
        }

        // Счётчики подтверждённых запросов
        Map<Long, Long> confirmedCounts = requestRepository.countConfirmedByEventIds(eventIds);

        // Счётчики просмотров
        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();

        List<ViewStatsDto> viewStatsList;
        try {
            viewStatsList = statsClient.getStats(LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    uris,
                    true
            );
        } catch (Exception e) {
            viewStatsList = List.of();
        }

        Map<Long, Long> viewsMap = viewStatsList.stream()
                .collect(Collectors.toMap(
                        v -> Long.parseLong(v.getUri().split("/")[2]),
                        ViewStatsDto::getHits
                ));
        log.info("Успешно получено {} подборок событий", compilationList.size());

        // Собираем DTO
        return compilationList.stream()
                .map(compilation -> {
                    CompilationDto dto = compilationMapper.toCompilationDto(compilation);
                    List<EventShortDto> updatedEvents = compilation.getEvents().stream()
                            .map(eventMapper::toEventShortDto)
                            .peek(e -> {
                                e.setConfirmedRequests(confirmedCounts.getOrDefault(e.getId(), 0L));
                                e.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                            })
                            .toList();
                    dto.setEvents(updatedEvents);
                    return dto;
                })
                .toList();
    }

    // получение подборки событие по его id
    @Override
    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена с id = " + compilationId));

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
        log.info("Подборка собятий по id = {} успешно получена.", compilationDto);
        return addConfirmedRequestsAndViews(compilationDto);
    }

    private CompilationDto addConfirmedRequestsAndViews(CompilationDto compilationDto) {
        for (EventShortDto eventDto : compilationDto.getEvents()) {
            // Добавить сonfirmedRequests к каждому событию
            eventDto.setConfirmedRequests(
                    requestRepository.countByEventIdAndStatus(eventDto.getId(), RequestStatus.CONFIRMED));

            // Добавить views к каждому событию
            List<String> uris = new ArrayList<>();

            // создаем uri для обращения к базе данных статистики
            uris.add("/events/" + eventDto.getId());
            List<ViewStatsDto> viewStatsDtoList = statsClient.getStats(LocalDateTime.now().minusYears(100), LocalDateTime.now(), uris, true);
            if (viewStatsDtoList.isEmpty()) {
                eventDto.setViews(0L);
            } else {
                eventDto.setViews(viewStatsDtoList.getFirst().getHits());
            }
        }
        return compilationDto;
    }
}
