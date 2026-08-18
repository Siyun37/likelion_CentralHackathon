package com.mcm.craft.product.dto;

import com.mcm.craft.product.Product;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record ProductResponse(
        Long productId,
        String name,
        Long price,
        String category,
        JsonNode colors,
        JsonNode sizes,
        String heritageText
) {

    public static ProductResponse from(Product product, ObjectMapper objectMapper) {
        try {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getCategory(),
                    objectMapper.readTree(product.getColors()),
                    objectMapper.readTree(product.getSizes()),
                    product.getHeritageText()
            );
        } catch (JacksonException e) {
            throw new IllegalStateException("Invalid product JSON data: productId=" + product.getId(), e);
        }
    }
}
