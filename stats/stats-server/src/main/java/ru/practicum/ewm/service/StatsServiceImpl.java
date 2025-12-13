package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatRepository statRepository;
    private final EndpointHitMapper endpointHitMapper;

    @Override
    @Transactional
    public void addHit(EndpointHitDto hitDto) {
        log.info("Начало работы StatsServiceImpl метод addHit");///
        log.info("Получены параметры");///
        log.info("hitDto = " + hitDto);///
        EndpointHit hit = endpointHitMapper.toHit(hitDto);
        log.info("После преобразования в сущность параметры следующие");///
        log.info("hit = " + hit);///
        statRepository.save(hit);
        log.info("Проверка сохранения");///
        List<ViewStatsDto> statistics;///
        statistics = statRepository.getUniqueStatsWithUris(LocalDateTime.now().minusYears(100), LocalDateTime.now(), List.of(hitDto.getUri()));///
        for (ViewStatsDto stat : statistics) {///
            System.out.println(stat);///
        }///
        log.info("Завершение работы StatsServiceImpl метод addHit");///
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("Начало работы StatsServiceImpl метод getStats");///
        log.info("Получены параметры");
        log.info("start = " + start);
        log.info("end = " + end);
        log.info("uri = " + uris.getFirst());
        log.info("unique = " + unique);///

        if (start == null) start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        if (end == null) end = LocalDateTime.now();
        if (end.isBefore(start)) {
            throw new ValidationException("Дата окончания не может быть позже даты начала выборки");
        }

        log.info("После обработки дат получаем следующее:");///
        log.info("start = " + start);///
        log.info("end = " + end);///

        List<ViewStatsDto> statistics;

        if (uris != null && !uris.isEmpty()) {
            if (unique) {
                statistics = statRepository.getUniqueStatsWithUris(start, end, uris);
            } else {
                statistics = statRepository.getStatsWithUris(start, end, uris);
            }
        } else {
            if (unique) {
                statistics = statRepository.getUniqueStats(start, end);
            } else {
                statistics = statRepository.getStats(start, end);
            }
        }
        log.info("Получена статистика:");///
        for (ViewStatsDto stat : statistics) {///
            System.out.println(stat);///
        }///
        return statistics;
    }
}
