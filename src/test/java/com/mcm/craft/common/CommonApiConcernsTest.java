package com.mcm.craft.common;

import com.mcm.craft.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통/횡단 관심사 테스트.
 */
class CommonApiConcernsTest extends ApiTestSupport {

    @Test
    @DisplayName("존재하지 않는 엔드포인트 호출 시 404 응답 " +
            "(GlobalExceptionHandler#handleNotFound가 NoResourceFoundException을 404로 매핑)")
    void unknownEndpoint_returns404() throws Exception {
        mockMvc.perform(get("/v1/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException은 400과 {message, errors} 포맷으로 매핑됨")
    void validationException_mapsTo400WithConsistentErrorFormat() throws Exception {
        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("DuplicatePreregistrationException은 409로 매핑됨 (PreregistrationControllerTest에서 흐름 검증)")
    void duplicateException_mapsTo409_documentedElsewhere() {
        // com.mcm.craft.prereg.PreregistrationControllerTest#register_duplicateCustomerAndProduct_returns409 참고
    }

    @Test
    @DisplayName("IllegalArgumentException은 400으로 매핑됨 (PreregistrationControllerTest#register_nonExistentCustomerId_returns400 참고). " +
            "단, 세션 프로필 조회 시 존재하지 않는 customer_id는 CustomerNotFoundException -> 404로 별도 매핑됨 " +
            "(SessionControllerTest#updateProfile_nonExistentCustomerId_returns404 참고)")
    void illegalArgumentException_mapsTo400_documentedElsewhere() {
    }

    @Test
    @DisplayName("CORS: 다른 origin에서의 preflight(OPTIONS) 요청이 허용되어야 함")
    void corsPreflight_fromDifferentOrigin_isAllowed() throws Exception {
        mockMvc.perform(options("/v1/products")
                        .header("Origin", "https://unity-webgl-build.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://unity-webgl-build.example.com"));
    }

    @Test
    @DisplayName("문법 오류가 있는 잘못된 형식의 JSON을 보내면 400 응답이어야 함(500 아님) " +
            "(GlobalExceptionHandler#handleMessageNotReadable가 HttpMessageNotReadableException을 400으로 매핑)")
    void malformedJson_returns400NotInternalServerError() throws Exception {
        mockMvc.perform(patch("/v1/sessions/" + java.util.UUID.randomUUID() + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Content-Type이 application/json이 아닌 요청은 415로 응답해야 함 " +
            "(GlobalExceptionHandler#handleUnsupportedMediaType가 HttpMediaTypeNotSupportedException을 415로 매핑)")
    void unsupportedContentType_returns415() throws Exception {
        mockMvc.perform(patch("/v1/sessions/" + java.util.UUID.randomUUID() + "/profile")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("gender=F"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
