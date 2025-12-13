//package ru.practicum.ewm;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.util.UriComponents;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//@Slf4j
//@Component
//public class StatsClient {
//    private final RestClient restClient;
//
//    public StatsClient(@Value("${stats-server.url}") String serverUrl) {
//        this.restClient = RestClient.builder().baseUrl(serverUrl).build();
//    }
//
//    public void addHit(EndpointHitDto hitDto) {
//        restClient.post()
//                .uri("/hit")
//                .body(hitDto)
//                .retrieve()
//                .toBodilessEntity();
//    }
//
//    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
//        System.out.println("Начало работы Stats-client");
//        StatsClient.log.info("Начало работы Stats-client-log.info");/////
//
//        UriComponentsBuilder builder = UriComponentsBuilder
//                .fromHttpUrl("http://stats-server:9090")
//                .queryParam("start", start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
//                .queryParam("end", end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//
//        if (uris != null && !uris.isEmpty()) {
//            builder.queryParam("uris", uris);
//        }
//
//        if (unique != null) {
//            builder.queryParam("unique", unique);
//        }
//        UriComponents uriComponents = builder.build();
//        System.out.println("builder: start = " + uriComponents.getQueryParams().getFirst("start") +
//                ", end = " + uriComponents.getQueryParams().getFirst("end") +
//                ", uris = " + uriComponents.getQueryParams().get("uris") +
//                ", unique = " + uriComponents.getQueryParams().getFirst("unique"));
//
//        return restClient.get()
//                .uri(builder.build().toUri())
//                .retrieve()
//                .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {
//                });
//    }
//}
//
//
//
//
