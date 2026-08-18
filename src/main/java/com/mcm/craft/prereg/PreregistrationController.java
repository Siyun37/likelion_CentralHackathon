package com.mcm.craft.prereg;

import com.mcm.craft.prereg.dto.PreregistrationRequest;
import com.mcm.craft.prereg.dto.PreregistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/preregistrations")
@RequiredArgsConstructor
@Tag(name = "Preregistration", description = "상품 사전등록 접수")
public class PreregistrationController {

    private final PreregistrationService preregistrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "사전등록 접수", description = "customer_id + product_id 조합으로 사전등록을 생성합니다. 동일 조합 재등록 시 409를 반환합니다.")
    public PreregistrationResponse register(@Valid @RequestBody PreregistrationRequest request) {
        return preregistrationService.register(request);
    }
}
