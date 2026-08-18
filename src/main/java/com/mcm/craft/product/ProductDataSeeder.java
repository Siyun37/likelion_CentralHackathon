package com.mcm.craft.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataSeeder implements CommandLineRunner {

    private static final String LAUNCH_STATUS_DUMMY = "dummy";

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
                product("Aren Tote", 890000L, "tote",
                        colors(color("black", true), color("beige", false)),
                        sizes("S", "M"),
                        "Aren 컬렉션의 시그니처 토트백. 견고한 새들 레더와 자연스러운 패티나가 특징입니다."),
                product("Solene Tote", 950000L, "tote",
                        colors(color("black", true), color("brown", true), color("ivory", false)),
                        sizes("M", "L"),
                        "매일의 사용을 위해 설계된 넉넉한 사이즈의 토트백. 내부 오거나이저 포켓을 포함합니다."),
                product("Iris Shoulder", 720000L, "shoulder",
                        colors(color("black", true), color("burgundy", false)),
                        sizes("One Size"),
                        "여성스러운 실루엣의 숄더백. 체인과 레더 스트랩을 겸용할 수 있습니다."),
                product("Noa Shoulder", 680000L, "shoulder",
                        colors(color("tan", true), color("black", true)),
                        sizes("S", "M"),
                        "미니멀한 디자인의 숄더백으로 캐주얼과 포멀 룩 모두에 어울립니다."),
                product("Pixie Mini", 450000L, "mini",
                        colors(color("black", true), color("red", false), color("beige", false)),
                        sizes("One Size"),
                        "컴팩트한 사이즈의 미니백. 짧은 외출이나 파티룩 포인트 아이템으로 적합합니다."),
                product("Luna Mini", 480000L, "mini",
                        colors(color("white", false), color("black", true)),
                        sizes("One Size"),
                        "둥근 실루엣이 특징인 미니백. 탈부착 가능한 숄더 스트랩이 포함됩니다.")
        );

        productRepository.saveAll(products);
        log.info("Seeded {} dummy products", products.size());
    }

    private Product product(String name, Long price, String category, String colorsJson, String sizesJson,
                             String heritageText) {
        return new Product(name, price, category, colorsJson, sizesJson, heritageText, LAUNCH_STATUS_DUMMY);
    }

    private Map<String, Object> color(String color, boolean modelSupported) {
        return Map.of("color", color, "model_supported", modelSupported);
    }

    @SafeVarargs
    private String colors(Map<String, Object>... colors) {
        return writeJson(List.of(colors));
    }

    private String sizes(String... sizes) {
        return writeJson(List.of(sizes));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize seed data", e);
        }
    }
}
