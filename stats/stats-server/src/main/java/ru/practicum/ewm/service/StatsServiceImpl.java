package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.EndpointHitDto;
import ru.practicum.ewm.ViewStatsDto;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.EndpointHit;
import ru.practicum.ewm.model.EndpointHitMapper;
import ru.practicum.ewm.repository.StatRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatRepository statRepository;
    private final EndpointHitMapper endpointHitMapper;

    @Override
    @Transactional
    public void addHit(EndpointHitDto hitDto) {
        EndpointHit hit = endpointHitMapper.toHit(hitDto);
        System.out.println("Работа метода addHit");
        System.out.println("Данные для сохранения");
        System.out.println(hit);
        statRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        System.out.println("Работа сервера статистики метод getStats");
        System.out.println("Получены параметры");
        System.out.println("start = " + start);
        System.out.println("end = " + end);
        for (String uri : uris) {
            System.out.println("uri = " + uri);
        }
        System.out.println("unique = " + unique);

        if (start == null) start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        if (end == null) end = LocalDateTime.now();
        if (end.isBefore(start)) {
            throw new ValidationException("Дата окончания не может быть позже даты начала выборки");
        }

        System.out.println("Применяемые параметры");
        System.out.println("start = " + start);
        System.out.println("end = " + end);
        for (String uri : uris) {
            System.out.println("uri = " + uri);
        }
        System.out.println("unique = " + unique);

        List<ViewStatsDto> statistics;

        if (uris != null && !uris.isEmpty()) {
            if (unique) {
                System.out.println("Сработало условие uris != null && !uris.isEmpty() && unique = true");
                statistics = statRepository.getUniqueStatsWithUris(start, end, uris);
            } else {
                System.out.println("Сработало условие uris != null && !uris.isEmpty() && unique = false");
                statistics = statRepository.getStatsWithUris(start, end, uris);
            }
        } else {
            if (unique) {
                System.out.println("Сработало условие uris == null && unique = true");
                statistics = statRepository.getUniqueStats(start, end);
            } else {
                System.out.println("Сработало условие uris == null && unique = false");
                statistics = statRepository.getStats(start, end);
            }
        }
        System.out.println(statistics.size());
        return statistics;
    }
}
