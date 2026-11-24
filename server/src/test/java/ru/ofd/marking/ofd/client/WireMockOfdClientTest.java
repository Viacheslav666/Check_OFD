package ru.ofd.marking.ofd.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ofd.marking.config.OfdProperties;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

@WireMockTest
class WireMockOfdClientTest {

    RateLimitedHttpOfdClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        OfdProperties props = new OfdProperties();
        props.setMode(OfdProperties.Mode.live);
        props.setBaseUrl(wm.getHttpBaseUrl());
        props.setApiKey("test-secret-key");
        props.setInn("7707083893");
        props.setRateLimitMs(10);
        props.setMaxRetries(2);
        props.setRetryBackoffMs(50);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        client = new RateLimitedHttpOfdClient(props, mapper);
    }

    @Test
    void listsKktsAndReceiptsInfoWithMarking(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/api/integration/v2/inn/7707083893/kkts"))
                .withQueryParam("AuthToken", equalTo("test-secret-key"))
                .willReturn(okJson("""
                        {"Status":"Success","Data":[{
                          "Id":"11111111-1111-1111-1111-111111111111",
                          "KktRegId":"0005956962031875",
                          "KktName":"Касса 1",
                          "FiscalAddress":"г. Москва, ул. Тестовая, 1",
                          "FiscalPlace":"Магазин",
                          "FnNumber":"9960440301019864"
                        }],"Elapsed":"00:00:00.01"}
                        """)));

        stubFor(get(urlPathMatching("/api/integration/v2/inn/7707083893/kkt/.*/receipts-info"))
                .willReturn(okJson("""
                        {"Status":"Success","Data":[{
                          "Id":"ee19889a-11b4-428d-83be-3dc9065dc28c",
                          "CDateUtc":"2022-10-01T11:16:08",
                          "Tag":3,
                          "IsBso":false,
                          "IsCorrection":false,
                          "OperationType":"Income",
                          "UserInn":"7707083893",
                          "KktRegNumber":"0005956962031875",
                          "FnNumber":"9960440301019864",
                          "DocNumber":8773,
                          "DocDateTime":"2022-10-01T11:16:00",
                          "DocRawId":"ee19889a-11b4-428d-83be-3dc9065dc28c",
                          "TotalSumm":923900,
                          "Items":[
                            {"Name":"носки","Price":16700,"Quantity":1,"Total":16700,"SubjectType":1,"ProductCode":{}},
                            {"Name":"ботинки","Price":799000,"Quantity":1,"Total":799000,"SubjectType":33,
                             "ProductCode":{"Code_GS_1M":"0104660184311215215osnRMP5"}}
                          ]
                        }],"Elapsed":"00:00:00.09"}
                        """)));

        List<OfdKktDto> kkts = client.listKkts("7707083893");
        assertThat(kkts).hasSize(1);
        assertThat(kkts.get(0).getKktRegId()).isEqualTo("0005956962031875");

        List<OfdReceiptDto> receipts = client.receiptsInfo(
                "7707083893", "0005956962031875",
                Instant.parse("2022-10-01T00:00:00Z"),
                Instant.parse("2022-10-02T00:00:00Z"));
        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).getItems()).hasSize(2);
        assertThat(receipts.get(0).getItems().get(1).getSubjectType()).isEqualTo(33);
        assertThat(receipts.get(0).getItems().get(1).getProductCode())
                .containsKey("Code_GS_1M");
    }

    @Test
    void retriesOn429() {
        stubFor(get(urlPathEqualTo("/api/integration/v2/inn/7707083893/kkts"))
                .inScenario("rate")
                .whenScenarioStateIs("Started")
                .willSetStateTo("ok")
                .willReturn(aResponse().withStatus(429).withBody("{\"Success\":false}")));
        stubFor(get(urlPathEqualTo("/api/integration/v2/inn/7707083893/kkts"))
                .inScenario("rate")
                .whenScenarioStateIs("ok")
                .willReturn(okJson("{\"Status\":\"Success\",\"Data\":[]}")));

        assertThat(client.listKkts("7707083893")).isEmpty();
    }
}
