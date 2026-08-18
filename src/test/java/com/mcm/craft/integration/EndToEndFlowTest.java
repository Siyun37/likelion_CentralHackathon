package com.mcm.craft.integration;

import com.jayway.jsonpath.JsonPath;
import com.mcm.craft.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 시나리오: ① 세션 생성 -> ② 프로필 등록 -> ③ 상품 목록 조회
 * -> ④ 이벤트 배치 전송 -> ⑤ 사전등록, 전체 흐름이 에러 없이 이어지는지 검증.
 */
class EndToEndFlowTest extends ApiTestSupport {

    @Test
    @DisplayName("세션 생성 -> 프로필 등록 -> 상품 조회 -> 이벤트 전송 -> 사전등록 전체 흐름이 정합성 있게 이어짐")
    void fullUserJourney_completesSuccessfully() throws Exception {
        // ① POST /v1/sessions
        String sessionBody = mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"device_type":"mobile","viewport_w":390,"viewport_h":844}
                                """))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customer_id").exists())
                .andReturn().getResponse().getContentAsString();
        String customerId = JsonPath.read(sessionBody, "$.customer_id");
        assertThat(customerId).isNotBlank();

        // ② PATCH profile
        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":"F","age":29,"height":163,"weight":50,"body_type":"slim"}
                                """))
                .andExpect(status().isOk());

        // ③ GET /v1/products
        String productsBody = mockMvc.perform(get("/v1/products").param("launch_status", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn().getResponse().getContentAsString();
        Number firstProductId = JsonPath.read(productsBody, "$[0].product_id");
        assertThat(firstProductId).isNotNull();

        // ④ POST /v1/events:batch (관람 로그)
        mockMvc.perform(post("/v1/events:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "events": [
                                    {
                                      "event_id": "evt-e2e-1",
                                      "event_type": "session_start",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:00Z"
                                    },
                                    {
                                      "event_id": "evt-e2e-2",
                                      "event_type": "product_view",
                                      "customer_id": "%s",
                                      "timestamp": "2026-08-18T10:00:05Z",
                                      "interaction_id": "int-e2e-1",
                                      "meta": {"product_id": %s, "duration_sec": 8}
                                    }
                                  ]
                                }
                                """.formatted(customerId, customerId, customerId, firstProductId)))
                .andExpect(status().isOk());

        // ⑤ POST /v1/preregistrations
        String preregBody = mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "product_id": %s,
                                  "name": "홍길동",
                                  "phone": "010-1234-5678",
                                  "consent": true
                                }
                                """.formatted(customerId, firstProductId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prereg_id").exists())
                .andExpect(jsonPath("$.created_at").exists())
                .andReturn().getResponse().getContentAsString();

        Number preregId = JsonPath.read(preregBody, "$.prereg_id");
        assertThat(preregId).isNotNull();
    }
}
