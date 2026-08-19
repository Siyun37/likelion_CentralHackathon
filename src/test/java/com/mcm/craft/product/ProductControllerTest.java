package com.mcm.craft.product;

import com.mcm.craft.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 3. GET /v1/products?launch_status=dummy
 *
 * data.sql이 애플리케이션 기동 시 launch_status="dummy" 상품 6개를 시드한다.
 * 시드는 컨텍스트 최초 기동 때 한 번, 별도 트랜잭션으로 커밋되므로
 * 각 테스트 메서드의 @Transactional 롤백과 무관하게 항상 조회 가능하다.
 */
class ProductControllerTest extends ApiTestSupport {

    private static final int SEEDED_DUMMY_PRODUCT_COUNT = 6;

    @Test
    @DisplayName("정상 호출 시 200 응답과 배열 반환")
    void getProducts_returnsArray() throws Exception {
        mockMvc.perform(get("/v1/products").param("launch_status", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("시드 데이터로 넣어둔 상품 개수(6개)가 정확히 반환됨")
    void getProducts_returnsSeededCount() throws Exception {
        mockMvc.perform(get("/v1/products").param("launch_status", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(SEEDED_DUMMY_PRODUCT_COUNT));
    }

    @Test
    @DisplayName("각 상품 객체에 product_id/name/price/category/colors[]/sizes[]/heritage_text 필드가 모두 존재")
    void getProducts_eachProductHasAllRequiredFields() throws Exception {
        mockMvc.perform(get("/v1/products").param("launch_status", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].product_id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].colors").isArray())
                .andExpect(jsonPath("$[0].sizes").isArray())
                .andExpect(jsonPath("$[0].heritage_text").exists());
    }

    @Test
    @DisplayName("colors 배열의 각 항목이 {color, model_supported} 구조로 역직렬화됨")
    void getProducts_colorsAreParsedAsColorModelSupportedStructure() throws Exception {
        mockMvc.perform(get("/v1/products").param("launch_status", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].colors[0].color").exists())
                .andExpect(jsonPath("$[0].colors[0].model_supported").isBoolean());
    }

    @Test
    @DisplayName("launch_status=live 처럼 다른 값을 주면 빈 배열(또는 필터링된 결과) 반환")
    void getProducts_withDifferentLaunchStatus_returnsFilteredOrEmptyResult() throws Exception {
        mockMvc.perform(get("/v1/products").param("launch_status", "live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("launch_status 파라미터 없이 호출 시 전체 상품 반환 " +
            "(ProductService: launchStatus == null이면 findAll() 호출 -> 전체 반환이 현재 스펙)")
    void getProducts_withoutLaunchStatusParam_returnsAllProducts() throws Exception {
        mockMvc.perform(get("/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(SEEDED_DUMMY_PRODUCT_COUNT));
    }
}
