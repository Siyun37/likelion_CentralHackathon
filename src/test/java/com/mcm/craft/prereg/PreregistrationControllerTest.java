package com.mcm.craft.prereg;

import com.mcm.craft.product.Product;
import com.mcm.craft.product.ProductRepository;
import com.mcm.craft.session.Customer;
import com.mcm.craft.session.CustomerRepository;
import com.mcm.craft.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4. POST /v1/preregistrations
 */
class PreregistrationControllerTest extends ApiTestSupport {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PreregistrationRepository preregistrationRepository;

    private Long existingProductId;

    @BeforeEach
    void setUpProduct() {
        // ProductDataSeeder가 기동 시 심어둔 시드 상품 중 하나를 사용
        existingProductId = productRepository.findAll().stream()
                .findFirst()
                .map(Product::getId)
                .orElseThrow(() -> new IllegalStateException("시드 상품이 존재하지 않습니다"));
    }

    private UUID createCustomer() {
        return customerRepository.save(Customer.create()).getId();
    }

    private String requestJson(UUID customerId, Long productId, String name, String phone, Boolean consent) {
        return """
                {
                  "customer_id": "%s",
                  "product_id": %s,
                  "name": %s,
                  "phone": %s,
                  "consent": %s
                }
                """.formatted(
                customerId,
                productId,
                name == null ? "null" : "\"" + name + "\"",
                phone == null ? "null" : "\"" + phone + "\"",
                consent
        );
    }

    @Test
    @DisplayName("정상 요청 시 201 응답, prereg_id/created_at 반환")
    void register_withValidPayload_returns201WithPreregIdAndCreatedAt() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prereg_id").exists())
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    @DisplayName("DB에 Preregistration row가 실제로 INSERT됨")
    void register_persistsPreregistrationRow() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isCreated());

        assertThat(preregistrationRepository.existsByCustomerIdAndProductId(customerId, existingProductId)).isTrue();
    }

    @Test
    @DisplayName("등록 성공 후 Customer.isIdentified=true, identifiedAt 세팅됨")
    void register_marksCustomerAsIdentified() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isCreated());

        Customer customer = customerRepository.findById(customerId).orElseThrow();
        assertThat(customer.isIdentified()).isTrue();
        assertThat(customer.getIdentifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일 customer_id + product_id로 재요청 시 409 Conflict")
    void register_duplicateCustomerAndProduct_returns409() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("consent:false로 보냈을 때 현재 동작 확인 " +
            "(DTO는 @NotNull Boolean이라 false도 유효값으로 통과됨 -> 현재는 201, consentAt만 null로 저장됨. " +
            "'필수 동의'가 정책이라면 별도 거부 로직 추가가 필요한지 기획 확인 필요)")
    void register_withConsentFalse_currentlyStillSucceeds() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "010-1234-5678", false)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("존재하지 않는 customer_id로 요청 시 처리 확인 " +
            "(현재 구현: CustomerRepository.findById 실패 -> IllegalArgumentException -> 400 매핑. FK 제약은 없음)")
    void register_nonExistentCustomerId_returns400() throws Exception {
        UUID unknownCustomerId = UUID.randomUUID();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(unknownCustomerId, existingProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 product_id로 요청 시 처리 확인 " +
            "(※ Preregistration.productId는 JPA 연관관계/FK가 없는 단순 Long 컬럼이라 " +
            "현재 구현은 상품 존재 여부를 검증하지 않고 그대로 201로 저장됨 - 데이터 무결성 갭 확인용)")
    void register_nonExistentProductId_currentlyStillSucceeds() throws Exception {
        UUID customerId = createCustomer();
        long unknownProductId = 999_999_999L;

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, unknownProductId, "홍길동", "010-1234-5678", true)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("name 누락 시 400 응답 (validation)")
    void register_missingName_returns400() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, null, "010-1234-5678", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("phone 누락 시 400 응답 (validation)")
    void register_missingPhone_returns400() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", null, true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("phone 형식이 이상한 값(문자 포함)일 때 현재 동작 확인 " +
            "(DTO에 @Pattern 등 형식 검증이 없어 현재는 그대로 통과/저장됨 - validation 부재 확인용)")
    void register_withInvalidPhoneFormat_currentlyPassesThrough() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/v1/preregistrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, existingProductId, "홍길동", "abcd-efgh", true)))
                .andExpect(status().isCreated());
    }
}
