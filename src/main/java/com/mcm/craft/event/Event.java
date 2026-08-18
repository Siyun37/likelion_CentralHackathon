package com.mcm.craft.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private UUID customerId;

    // "timestamp"는 SQL 예약어라 event_timestamp로 매핑(Hibernate 기본 네이밍 전략에 의해 자동 변환됨)
    private LocalDateTime eventTimestamp;

    private String interactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String meta;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    public Event(String eventId, String eventType, UUID customerId, LocalDateTime eventTimestamp,
                 String interactionId, String meta) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.customerId = customerId;
        this.eventTimestamp = eventTimestamp;
        this.interactionId = interactionId;
        this.meta = meta;
        this.receivedAt = LocalDateTime.now();
    }
}
