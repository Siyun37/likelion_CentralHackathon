package com.mcm.craft.event;

import com.mcm.craft.event.dto.EventBatchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Event", description = "행동/관람 로그 이벤트 배치 수집")
public class EventController {

    private final EventService eventService;

    @PostMapping("/events:batch")
    @Operation(summary = "이벤트 배치 전송", description = "하나 이상의 행동 로그 이벤트를 배열로 받아 서버에 기록합니다.")
    public ResponseEntity<Void> processBatch(@Valid @RequestBody EventBatchRequest request) {
        eventService.processBatch(request);
        return ResponseEntity.ok().build();
    }
}
