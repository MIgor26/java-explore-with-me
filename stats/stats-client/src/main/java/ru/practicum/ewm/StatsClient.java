package ru.practicum.ewm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class StatsClient {

    private final String http = "http";

    private String serverUri;
    private RestClient restClient;

    @Autowired
    public StatsClient(@Value("${stats-server.uri:http://stats-server:9090}") String serverUri) {
        this.serverUri = serverUri;
        this.restClient = RestClient.create();
    }

    public void addHit(EndpointHitDto hitDto) {
        String uri = UriComponentsBuilder.newInstance()
                .uri(URI.create(serverUri))
                .path("/hit")
                .toUriString();

        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(hitDto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .toBodilessEntity();
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String uriWithParams = UriComponentsBuilder.newInstance()
                .uri(URI.create(serverUri))
                .path("/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        return restClient.get()
                .uri(uriWithParams).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
