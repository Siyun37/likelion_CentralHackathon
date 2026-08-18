package com.mcm.craft.product;

import com.mcm.craft.product.dto.ProductResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public List<ProductResponse> getProducts(String launchStatus) {
        List<Product> products = launchStatus == null
                ? productRepository.findAll()
                : productRepository.findByLaunchStatus(launchStatus);
        return products.stream()
                .map(product -> ProductResponse.from(product, objectMapper))
                .toList();
    }
}
