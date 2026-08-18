package com.mcm.craft.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mcm.craft.support.ApiTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 5. POST /v1/events:batch
 */
class EventControllerTest extends ApiTestSupport {

    @Autowired
    private EventRepository eventRepository;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger eventServiceLogger;

    @BeforeEach
    void attachLogAppender() {
        eventServiceLogger = (Logger) LoggerFactory.getLogger(EventService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        eventServiceLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        eventServiceLogger.detachAppender(logAppender);
    }

    private static final String ENDPOINT = "/v1/events:batch";

    @Test
    @DisplayName("정상 이벤트 배열(1개 이상) 전송 시 200 응답")
    void processBatch_withEvents_returns200() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-1",
                                      "event_type": "product_view",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z",
                                      "interaction_id": "int-1",
                                      "meta": {"product_id": 1, "duration_sec": 12}
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이벤트 전송 시 Event 테이블에 실제로 row가 INSERT되고 필드가 정확히 매핑됨")
    void processBatch_persistsEventWithAllFields() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-persist-1",
                                      "event_type": "product_view",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z",
                                      "interaction_id": "int-persist-1",
                                      "meta": {"product_id": 1, "duration_sec": 12}
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId)))
                .andExpect(status().isOk());

        List<Event> saved = eventRepository.findAll().stream()
                .filter(e -> e.getEventId().equals("evt-persist-1"))
                .toList();
        assertThat(saved).hasSize(1);

        Event event = saved.get(0);
        assertThat(event.getEventType()).isEqualTo("product_view");
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getInteractionId()).isEqualTo("int-persist-1");
        assertThat(event.getEventTimestamp()).isEqualTo(java.time.LocalDateTime.of(2026, 8, 18, 10, 0, 0));
        assertThat(event.getMeta()).contains("\"product_id\":1").contains("\"duration_sec\":12");
        assertThat(event.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일 event_id로 재전송(중복)해도 DB에는 한 건만 저장됨")
    void processBatch_duplicateEventId_isSkipped() throws Exception {
        UUID customerId = UUID.randomUUID();
        String body = """
                {
                  "customer_id": "%s",
                  "events": [
                    {
                      "event_id": "evt-dup-1",
                      "event_type": "product_view",
                      "customer_id": "%s",
                      "timestamp": "2026-08-18T10:00:00Z"
                    }
                  ]
                }
                """.formatted(customerId, customerId);

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        long count = eventRepository.findAll().stream()
                .filter(e -> e.getEventId().equals("evt-dup-1"))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("timestamp/interaction_id/meta가 없는 이벤트(session_start 등)도 null 필드로 정상 저장됨")
    void processBatch_withoutOptionalFields_persistsWithNulls() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-no-optional",
                                      "event_type": "session_start",
                                      "customer_id": "%s"
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId)))
                .andExpect(status().isOk());

        Event event = eventRepository.findAll().stream()
                .filter(e -> e.getEventId().equals("evt-no-optional"))
                .findFirst()
                .orElseThrow();
        assertThat(event.getEventTimestamp()).isNull();
        assertThat(event.getInteractionId()).isNull();
        assertThat(event.getMeta()).isNull();
        assertThat(event.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("events 배열이 비어있는 경우([]) 현재 동작 확인 " +
            "(※ EventBatchRequest.events는 @NotEmpty이므로 현재는 400이 반환됨. " +
            "체크리스트가 기대하는 '에러 없이 200'과 배치되므로 정책 확인 필요)")
    void processBatch_withEmptyEvents_currentlyReturns400() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customer_id": "%s", "events": []}
                                """.formatted(customerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("각 이벤트 필드(event_id/event_type/customer_id/timestamp/interaction_id/meta)가 로그에 정상 파싱되어 찍힘")
    void processBatch_logsAllEventFields() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-42",
                                      "event_type": "product_view",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z",
                                      "interaction_id": "int-42",
                                      "meta": {"product_id": 7}
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId)))
                .andExpect(status().isOk());

        String logged = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertThat(logged).contains("evt-42");
        assertThat(logged).contains("product_view");
        assertThat(logged).contains(customerId.toString());
        assertThat(logged).contains("int-42");
        assertThat(logged).contains("product_id");
    }

    @Test
    @DisplayName("meta 안의 다양한 필드(product_id, duration_sec, occurrence_index)가 타입 손실 없이 로그에 찍힘")
    void processBatch_logsMetaFieldsWithoutTypeLoss() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-meta",
                                      "event_type": "product_view",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z",
                                      "interaction_id": "int-meta",
                                      "meta": {"product_id": 3, "duration_sec": 45.5, "occurrence_index": 2, "featured": true}
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId)))
                .andExpect(status().isOk());

        String logged = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertThat(logged).contains("product_id=3");
        assertThat(logged).contains("duration_sec=45.5");
        assertThat(logged).contains("occurrence_index=2");
        assertThat(logged).contains("featured=true");
    }

    @Test
    @DisplayName("session_start/session_end처럼 interaction_id/occurrence_index가 없는 이벤트도 에러 없이 처리")
    void processBatch_withoutInteractionIdOrOccurrenceIndex_returns200() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-session-start",
                                      "event_type": "session_start",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z"
                                    },
                                    {
                                      "event_id": "evt-session-end",
                                      "event_type": "session_end",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:05:00Z"
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId, customerId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("event_type이 명세서에 없는 낯선 값이어도 서버가 죽지 않고 200 처리")
    void processBatch_withUnknownEventType_stillReturns200() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-unknown",
                                      "event_type": "some_totally_unknown_event_type",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z"
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("대량 이벤트(100개 이상)를 한 번에 보내도 타임아웃 없이 처리")
    void processBatch_withLargeNumberOfEvents_isProcessedSuccessfully() throws Exception {
        UUID customerId = UUID.randomUUID();
        int eventCount = 150;

        String events = IntStream.range(0, eventCount)
                .mapToObj(i -> """
                        {
                          "event_id": "evt-bulk-%d",
                          "event_type": "product_view",
                          "customer_id": "%s",
                          "timestamp": "2026-08-18T10:00:00Z",
                          "meta": {"product_id": %d}
                        }
                        """.formatted(i, customerId, i))
                .collect(Collectors.joining(","));

        String requestBody = """
                {"customer_id": "%s", "events": [%s]}
                """.formatted(customerId, events);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        long count = logAppender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("evt-bulk-"))
                .count();
        assertThat(count).isEqualTo(eventCount);

        long savedCount = eventRepository.findAll().stream()
                .filter(e -> e.getEventId().startsWith("evt-bulk-"))
                .count();
        assertThat(savedCount).isEqualTo(eventCount);
    }
}
