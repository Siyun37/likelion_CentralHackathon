package com.mcm.craft.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공통 테스트 베이스.
 * - @SpringBootTest: 전체 컨텍스트(H2 인메모리 DB 포함) 로딩
 * - @Transactional: 각 테스트 메서드 종료 시 자동 롤백 -> 테스트 간 데이터 독립성 보장
 *   (단, data.sql로 심어진 시드 상품 데이터는 컨텍스트 최초 기동 시 별도 트랜잭션으로 커밋되므로
 *    각 테스트의 롤백과 무관하게 항상 존재한다)
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;
}
