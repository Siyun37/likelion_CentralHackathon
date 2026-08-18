package com.mcm.craft.session;

import com.mcm.craft.session.dto.CreateSessionRequest;
import com.mcm.craft.session.dto.CreateSessionResponse;
import com.mcm.craft.session.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Session", description = "익명 세션 발급 및 방문객 프로필 등록")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "익명 세션 생성", description = "device_type/viewport 정보를 선택적으로 받아 새 customer_id(UUID)를 발급합니다.")
    public ResponseEntity<CreateSessionResponse> createSession(
            @RequestBody(required = false) CreateSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.createSession());
    }

    @PatchMapping("/{customerId}/profile")
    @Operation(summary = "방문객 프로필 갱신", description = "gender/age/height/weight/body_type 중 전달된 필드만 갱신합니다(null은 skip).")
    public ResponseEntity<Void> updateProfile(
            @PathVariable UUID customerId,
            @RequestBody UpdateProfileRequest request
    ) {
        sessionService.updateProfile(customerId, request);
        return ResponseEntity.ok().build();
    }
}
