-- Product 시드 데이터
-- 앱 기동 시마다 실행되며(spring.sql.init.mode=always), 이름을 기준으로
-- 기존 행이 있으면 UPDATE, 없으면 INSERT한다.
-- DELETE 후 재삽입하지 않는 이유: id는 IDENTITY(자동 증가) 컬럼이라
-- 삭제 후 다시 넣으면 새로운 id가 부여되어 이미 발급된 product_id를
-- 참조하는 event(meta.product_id)나 preregistration(product_id) 데이터와
-- 정합성이 깨지기 때문. UPDATE로 처리하면 기존 id가 그대로 유지된다.

-- 1. Stark 사이드 스터드 비세토스 백팩
UPDATE products SET
    price = 1890000,
    category = 'backpack',
    colors = '[{"color":"black","model_supported":true},{"color":"cognac","model_supported":true},{"color":"green","model_supported":true}]',
    sizes = '["16 x 33 x 41 cm"]',
    heritage_text = '피라미드 모양 스터드 장식과 천연 나파 가죽 트림이 특징인 비세토스 모노그램 캔버스 백팩',
    launch_status = 'dummy'
WHERE name = 'Stark 사이드 스터드 비세토스 백팩';

INSERT INTO products (name, price, category, colors, sizes, heritage_text, launch_status)
SELECT 'Stark 사이드 스터드 비세토스 백팩', 1890000, 'backpack',
       '[{"color":"black","model_supported":true},{"color":"cognac","model_supported":true},{"color":"green","model_supported":true}]',
       '["16 x 33 x 41 cm"]',
       '피라미드 모양 스터드 장식과 천연 나파 가죽 트림이 특징인 비세토스 모노그램 캔버스 백팩',
       'dummy'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Stark 사이드 스터드 비세토스 백팩');

-- 2. Ella 맥시 모노그램 레더 보스턴 백
UPDATE products SET
    price = 1690000,
    category = 'boston',
    colors = '[{"color":"lotus","model_supported":true}]',
    sizes = '["12 x 22 x 15 cm"]',
    heritage_text = '엠보싱 처리된 맥시 비세토스 모노그램과 바이에른 다이아몬드 가죽 참이 어우러진 천연 풀그레인 레더 보스턴 백',
    launch_status = 'dummy'
WHERE name = 'Ella 맥시 모노그램 레더 보스턴 백';

INSERT INTO products (name, price, category, colors, sizes, heritage_text, launch_status)
SELECT 'Ella 맥시 모노그램 레더 보스턴 백', 1690000, 'boston',
       '[{"color":"lotus","model_supported":true}]',
       '["12 x 22 x 15 cm"]',
       '엠보싱 처리된 맥시 비세토스 모노그램과 바이에른 다이아몬드 가죽 참이 어우러진 천연 풀그레인 레더 보스턴 백',
       'dummy'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Ella 맥시 모노그램 레더 보스턴 백');

-- 3. Aren 다이아몬드 퀼팅 레더 백팩
UPDATE products SET
    price = 2690000,
    category = 'backpack',
    colors = '[{"color":"black","model_supported":true}]',
    sizes = '["20 x 29 x 45 cm"]',
    heritage_text = '다이아몬드 모티프 퀼딩 나파가죽 백팩',
    launch_status = 'dummy'
WHERE name = 'Aren 다이아몬드 퀼팅 레더 백팩';

INSERT INTO products (name, price, category, colors, sizes, heritage_text, launch_status)
SELECT 'Aren 다이아몬드 퀼팅 레더 백팩', 2690000, 'backpack',
       '[{"color":"black","model_supported":true}]',
       '["20 x 29 x 45 cm"]',
       '다이아몬드 모티프 퀼딩 나파가죽 백팩',
       'dummy'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Aren 다이아몬드 퀼팅 레더 백팩');

-- 4. Dessau 비세토스 드로우스트링 백
UPDATE products SET
    price = 1550000,
    category = 'bucket',
    colors = '[{"color":"white","model_supported":true},{"color":"cognac","model_supported":true},{"color":"black","model_supported":true}]',
    sizes = '["14 x 28 x 23 cm"]',
    heritage_text = '매칭되는 지퍼 파우치와 천연 가죽 트림이 더해진 비세토스 모노그램 캔버스 버킷백',
    launch_status = 'dummy'
WHERE name = 'Dessau 비세토스 드로우스트링 백';

INSERT INTO products (name, price, category, colors, sizes, heritage_text, launch_status)
SELECT 'Dessau 비세토스 드로우스트링 백', 1550000, 'bucket',
       '[{"color":"white","model_supported":true},{"color":"cognac","model_supported":true},{"color":"black","model_supported":true}]',
       '["14 x 28 x 23 cm"]',
       '매칭되는 지퍼 파우치와 천연 가죽 트림이 더해진 비세토스 모노그램 캔버스 버킷백',
       'dummy'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Dessau 비세토스 드로우스트링 백');

-- 5. Tracy 비세토스 레더 믹스 사첼
UPDATE products SET
    price = 1850000,
    category = 'satchel',
    colors = '[{"color":"cognac","model_supported":true}]',
    sizes = '["9 x 25 x 19 cm"]',
    heritage_text = '라우렐 락 잠금장치와 이탈리안 카프스킨 레더 트림이 더해진 비세토스 모노그램 캔버스 사첼 백',
    launch_status = 'dummy'
WHERE name = 'Tracy 비세토스 레더 믹스 사첼';

INSERT INTO products (name, price, category, colors, sizes, heritage_text, launch_status)
SELECT 'Tracy 비세토스 레더 믹스 사첼', 1850000, 'satchel',
       '[{"color":"cognac","model_supported":true}]',
       '["9 x 25 x 19 cm"]',
       '라우렐 락 잠금장치와 이탈리안 카프스킨 레더 트림이 더해진 비세토스 모노그램 캔버스 사첼 백',
       'dummy'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Tracy 비세토스 레더 믹스 사첼');

-- 6. Pina 비세토스 스터드 장식 토트
UPDATE products SET
    price = 1690000,
    category = 'tote',
    colors = '[{"color":"cognac","model_supported":true}]',
    sizes = '["13 x 30 x 24 cm"]',
    heritage_text = '나파 가죽 트림과 메탈 스터드 장식이 돋보이는 비세토스 모노그램 캔버스 토트백',
    launch_status = 'dummy'
WHERE name = 'Pina 비세토스 스터드 장식 토트';

INSERT INTO products (name, price, category, colors, sizes, heritage_text, launch_status)
SELECT 'Pina 비세토스 스터드 장식 토트', 1690000, 'tote',
       '[{"color":"cognac","model_supported":true}]',
       '["13 x 30 x 24 cm"]',
       '나파 가죽 트림과 메탈 스터드 장식이 돋보이는 비세토스 모노그램 캔버스 토트백',
       'dummy'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Pina 비세토스 스터드 장식 토트');
