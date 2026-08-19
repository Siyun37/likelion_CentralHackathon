package com.mcm.craft.session;

import com.mcm.craft.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 1. POST /v1/sessions
 * 2. PATCH /v1/sessions/{customerId}/profile
 */
class SessionControllerTest extends ApiTestSupport {

    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^cus_\\d{4,}$");

    @Autowired
    private CustomerRepository customerRepository;

    // ---------------------------------------------------------------
    // POST /v1/sessions
    // ---------------------------------------------------------------

    @Test
    @DisplayName("정상 요청 시 200 응답과 cus_0001 형식의 customer_id 반환")
    void createSession_returnsFormattedCustomerId() throws Exception {
        String body = mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"device_type":"mobile","viewport_w":390,"viewport_h":844}
                                """))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customer_id").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("customer_id");
        String customerId = com.jayway.jsonpath.JsonPath.read(body, "$.customer_id");
        assertThat(CUSTOMER_ID_PATTERN.matcher(customerId).matches()).isTrue();
    }

    @Test
    @DisplayName("응답 바디에 session_token 필드가 존재하지 않아야 함")
    void createSession_responseDoesNotContainSessionToken() throws Exception {
        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.session_token").doesNotExist())
                .andExpect(jsonPath("$.sessionToken").doesNotExist());
    }

    @Test
    @DisplayName("device_type/viewport_w/viewport_h 없는 빈 바디로 요청해도 정상 동작 (선택 필드)")
    void createSession_worksWithEmptyBody() throws Exception {
        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customer_id").exists());
    }

    @Test
    @DisplayName("바디 없이(No body) 요청해도 정상 동작 (선택 필드, @RequestBody required=false)")
    void createSession_worksWithNoBodyAtAll() throws Exception {
        mockMvc.perform(post("/v1/sessions"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customer_id").exists());
    }

    @Test
    @DisplayName("호출할 때마다 customer_id가 매번 다르게 생성됨 (중복 없는 순차 ID)")
    void createSession_generatesUniqueCustomerIdEachCall() throws Exception {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String body = mockMvc.perform(post("/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn().getResponse().getContentAsString();
            String customerId = com.jayway.jsonpath.JsonPath.read(body, "$.customer_id");
            ids.add(customerId);
        }
        assertThat(ids).hasSize(20);
    }

    @Test
    @DisplayName("DB에 Customer row가 실제 INSERT되며 isIdentified=false, 프로필 필드는 전부 null")
    void createSession_persistsCustomerWithDefaults() throws Exception {
        String body = mockMvc.perform(post("/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        String customerId = com.jayway.jsonpath.JsonPath.read(body, "$.customer_id");

        Optional<Customer> saved = customerRepository.findById(customerId);
        assertThat(saved).isPresent();
        Customer customer = saved.get();
        assertThat(customer.isIdentified()).isFalse();
        assertThat(customer.getIdentifiedAt()).isNull();
        assertThat(customer.getGender()).isNull();
        assertThat(customer.getAge()).isNull();
        assertThat(customer.getHeight()).isNull();
        assertThat(customer.getWeight()).isNull();
        assertThat(customer.getBodyType()).isNull();
        assertThat(customer.getCreatedAt()).isNotNull();
    }

    // ---------------------------------------------------------------
    // PATCH /v1/sessions/{customerId}/profile
    // ---------------------------------------------------------------

    private String createCustomer() throws Exception {
        String body = mockMvc.perform(post("/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.customer_id");
    }

    @Test
    @DisplayName("존재하는 customer_id로 정상 프로필 전송 시 200 및 DB 반영")
    void updateProfile_withFullPayload_persistsToDb() throws Exception {
        String customerId = createCustomer();

        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":"F","age":28,"height":165,"weight":52,"body_type":"slim"}
                                """))
                .andExpect(status().isOk());

        Customer customer = customerRepository.findById(customerId).orElseThrow();
        assertThat(customer.getGender()).isEqualTo("F");
        assertThat(customer.getAge()).isEqualTo(28);
        assertThat(customer.getHeight()).isEqualTo(165);
        assertThat(customer.getWeight()).isEqualTo(52);
        assertThat(customer.getBodyType()).isEqualTo("slim");
    }

    @Test
    @DisplayName("일부 필드만 보내도(나머지는 null) 보낸 필드만 정상 반영됨")
    void updateProfile_withPartialPayload_onlyUpdatesProvidedFields() throws Exception {
        String customerId = createCustomer();

        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":"M"}
                                """))
                .andExpect(status().isOk());

        Customer customer = customerRepository.findById(customerId).orElseThrow();
        assertThat(customer.getGender()).isEqualTo("M");
        assertThat(customer.getAge()).isNull();
        assertThat(customer.getHeight()).isNull();
        assertThat(customer.getWeight()).isNull();
        assertThat(customer.getBodyType()).isNull();
    }

    @Test
    @DisplayName("모든 필드를 null로 보내는 Skip 케이스도 정상 처리(200), 값 변경 없음")
    void updateProfile_withAllNullFields_isNoOpAndReturns200() throws Exception {
        String customerId = createCustomer();

        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":null,"age":null,"height":null,"weight":null,"body_type":null}
                                """))
                .andExpect(status().isOk());

        Customer customer = customerRepository.findById(customerId).orElseThrow();
        assertThat(customer.getGender()).isNull();
        assertThat(customer.getAge()).isNull();
        assertThat(customer.getHeight()).isNull();
        assertThat(customer.getWeight()).isNull();
        assertThat(customer.getBodyType()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 customer_id로 요청 시 404 응답 " +
            "(SessionService가 CustomerNotFoundException을 던지고 GlobalExceptionHandler가 404로 매핑)")
    void updateProfile_nonExistentCustomerId_returns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender":"F"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("height에 비현실적인 값(음수/300)을 넣었을 때의 현재 동작 확인 " +
            "(DTO에 검증 애노테이션이 없어 현재는 그대로 통과/저장됨 - validation 부재 확인용)")
    void updateProfile_withUnrealisticHeight_currentlyPassesThrough() throws Exception {
        String customerId = createCustomer();

        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"height":300}
                                """))
                .andExpect(status().isOk());

        String customerId2 = createCustomer();
        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"height":-10}
                                """))
                .andExpect(status().isOk());

        assertThat(customerRepository.findById(customerId).orElseThrow().getHeight()).isEqualTo(300);
        assertThat(customerRepository.findById(customerId2).orElseThrow().getHeight()).isEqualTo(-10);
    }

    @Test
    @DisplayName("age/height/weight에 문자열 등 잘못된 타입을 보내면 400 응답이어야 함 " +
            "(GlobalExceptionHandler#handleMessageNotReadable가 HttpMessageNotReadableException을 400으로 매핑)")
    void updateProfile_withWrongType_returns400() throws Exception {
        String customerId = createCustomer();

        mockMvc.perform(patch("/v1/sessions/{customerId}/profile", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"age":"twenty"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
