package ru.ofd.marking.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import ru.ofd.marking.IntegrationTestBase;
import ru.ofd.marking.domain.repo.ReceiptRepository;
import ru.ofd.marking.web.dto.TradePointStatsDto;

class CollectionE2ETest extends IntegrationTestBase {

    @Autowired CollectionService collectionService;
    @Autowired ReceiptRepository receiptRepository;
    @Autowired TestRestTemplate rest;

    @Test
    void collectionToMetricsFlow() {
        CollectionService.SyncStatus status = collectionService.runSync();
        assertThat(status.message()).startsWith("OK");
        assertThat(receiptRepository.count()).isGreaterThan(0);

        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant from = to.minus(120, ChronoUnit.DAYS);
        ResponseEntity<List<TradePointStatsDto>> resp = rest.exchange(
                "/api/trade-points?from={from}&to={to}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                from, to);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotEmpty();
        assertThat(resp.getBody().get(0).totalReceipts()).isGreaterThan(0);

        ResponseEntity<List<TradePointStatsDto>> worst = rest.exchange(
                "/api/trade-points/worst?from={from}&to={to}&threshold=100",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                from, to);
        assertThat(worst.getBody()).isNotEmpty();
    }
}
