package com.mcm.craft.event;

import com.mcm.craft.event.dto.EventBatchRequest;
import com.mcm.craft.event.dto.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processBatch(EventBatchRequest request) {
        for (EventDto event : request.events()) {
            log.info("[event] customerId={}, eventId={}, eventType={}, timestamp={}, interactionId={}, meta={}",
                    request.customerId(), event.eventId(), event.eventType(), event.timestamp(),
                    event.interactionId(), event.meta());

            if (eventRepository.existsByEventId(event.eventId())) {
                log.debug("Duplicate event skipped: eventId={}", event.eventId());
                continue;
            }

            eventRepository.save(new Event(
                    event.eventId(),
                    event.eventType(),
                    event.customerId(),
                    toLocalDateTime(event.timestamp()),
                    event.interactionId(),
                    toJson(event.meta())
            ));
        }
    }

    private LocalDateTime toLocalDateTime(Instant timestamp) {
        return timestamp == null ? null : LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
    }

    private String toJson(Map<String, Object> meta) {
        return meta == null ? null : objectMapper.writeValueAsString(meta);
    }
}
