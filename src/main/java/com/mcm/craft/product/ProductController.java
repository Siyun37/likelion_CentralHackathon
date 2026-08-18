package com.mcm.craft.product;

import com.mcm.craft.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "전시/사전등록 대상 상품 목록 조회")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "launch_status로 필터링된 상품 목록을 반환합니다. 생략 시 전체 상품을 반환합니다.")
    public List<ProductResponse> getProducts(
            @Parameter(example = "dummy") @RequestParam(name = "launch_status", required = false) String launchStatus
    ) {
        return productService.getProducts(launchStatus);
    }
}
