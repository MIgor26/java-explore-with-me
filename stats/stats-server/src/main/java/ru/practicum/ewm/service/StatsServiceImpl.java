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
        EndpointHit hit = endpointHitMapper.toHit(hitDto);
        statRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {

        // Требование тестов Гитхаб
        if (start == null || end == null) {
            throw new ValidationException("Дата старта и окончания должны быть указаны");
        }

        if (end.isBefore(start)) {
            throw new ValidationException("Дата окончания не может быть позже даты начала выборки");
        }

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
        return statistics;
    }
}
