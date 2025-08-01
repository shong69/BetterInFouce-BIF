-- 애플리케이션 시작 시 자동으로 실행되는 데이터 초기화 SQL

-- emotion_statistics_template 테이블 생성
CREATE TABLE IF NOT EXISTS emotion_statistics_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    okay_range VARCHAR(10) NOT NULL,
    good_range VARCHAR(10) NOT NULL,
    angry_range VARCHAR(10) NOT NULL,
    down_range VARCHAR(10) NOT NULL,
    great_range VARCHAR(10) NOT NULL,
    statistics_text TEXT NOT NULL
);

-- guardian_advice_template 테이블 생성
CREATE TABLE IF NOT EXISTS guardian_advice_template (
    advice_template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    okay_range VARCHAR(10) NOT NULL,
    good_range VARCHAR(10) NOT NULL,
    angry_range VARCHAR(10) NOT NULL,
    down_range VARCHAR(10) NOT NULL,
    great_range VARCHAR(10) NOT NULL,
    advice_text TEXT NOT NULL
);

-- emotion_statistics 테이블이 없으면 생성 (H2용)
CREATE TABLE IF NOT EXISTS emotion_statistics (
    statistics_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bif_id BIGINT NOT NULL,
    year_value INT NOT NULL,
    month_value INT NOT NULL,
    emotion_statistics_text TEXT NOT NULL,
    guardian_advice_text TEXT NOT NULL,
    emotion_counts VARCHAR(1000),
    top_keywords VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_bif_id ON emotion_statistics(bif_id);
CREATE INDEX IF NOT EXISTS idx_year_month ON emotion_statistics(year_value, month_value);
CREATE INDEX IF NOT EXISTS idx_updated_at ON emotion_statistics(updated_at);

-- 유니크 제약조건
ALTER TABLE emotion_statistics ADD CONSTRAINT IF NOT EXISTS uk_bif_year_month UNIQUE (bif_id, year_value, month_value);

-- 감정 분석 템플릿 데이터 삽입 (다양한 감정 비율 조합)
INSERT INTO emotion_statistics_template (okay_range, good_range, angry_range, down_range, great_range, statistics_text) VALUES
('0-30', '31-60', '0-30', '0-30', '0-30', '이번 달은 좋은 감정이 가장 많이 나타났습니다. 전반적으로 긍정적인 기분을 유지하셨네요. 이런 좋은 감정이 계속 이어지길 바랍니다.'),
('31-60', '0-30', '0-30', '0-30', '0-30', '이번 달은 평범한 감정이 가장 많았습니다. 안정적이고 일상적인 한 달을 보내신 것 같아요. 이런 안정감은 건강한 정신 상태를 나타냅니다.'),
('0-30', '0-30', '31-60', '0-30', '0-30', '이번 달에는 화난 감정이 많이 나타났습니다. 스트레스가 많았거나 힘든 일이 있었나요? 이런 감정은 자연스러운 반응이니 너무 걱정하지 마세요.'),
('0-30', '0-30', '0-30', '31-60', '0-30', '이번 달에는 우울한 감정이 많이 나타났습니다. 마음이 많이 힘드셨나요? 이런 감정을 느끼는 것은 정상입니다. 주변 사람들과 대화해보세요.'),
('0-30', '0-30', '0-30', '0-30', '31-60', '이번 달은 최고의 감정이 가장 많이 나타났습니다! 정말 행복하고 만족스러운 한 달을 보내셨네요. 이런 기분이 계속 이어지길 바랍니다!'),
('61-100', '0-30', '0-30', '0-30', '0-30', '이번 달은 평범한 감정이 압도적으로 많았습니다. 매우 안정적이고 일상적인 한 달을 보내신 것 같네요.'),
('0-30', '61-100', '0-30', '0-30', '0-30', '이번 달은 좋은 감정이 압도적으로 많았습니다! 정말 행복하고 만족스러운 한 달을 보내셨네요.');

-- 보호자 조언 템플릿 데이터 삽입
INSERT INTO guardian_advice_template (okay_range, good_range, angry_range, down_range, great_range, advice_text) VALUES
('0-30', '31-60', '0-30', '0-30', '0-30', 'BIF가 매우 긍정적인 감정을 많이 느끼고 있습니다. 이런 좋은 기분을 유지할 수 있도록 지지해주세요. 함께 즐거운 활동을 해보는 것도 좋겠어요.'),
('31-60', '0-30', '0-30', '0-30', '0-30', 'BIF가 안정적인 감정 상태를 유지하고 있습니다. 이런 균형잡힌 상태는 건강한 정신 상태를 나타냅니다. 현재의 안정감을 지지해주세요.'),
('0-30', '0-30', '31-60', '0-30', '0-30', 'BIF가 화난 감정을 많이 경험하고 있습니다. 스트레스나 불만이 있을 수 있어요. 따뜻한 관심과 대화를 통해 원인을 파악해보세요.'),
('0-30', '0-30', '0-30', '31-60', '0-30', 'BIF가 우울한 감정을 많이 느끼고 있습니다. 마음이 많이 힘들어하고 있을 수 있어요. 따뜻한 관심과 지지를 통해 도움을 주세요.'),
('0-30', '0-30', '0-30', '0-30', '31-60', 'BIF가 최고의 감정을 많이 느끼고 있습니다! 정말 행복하고 만족스러운 상태네요. 이런 좋은 기분이 계속 이어지도록 지지해주세요.'),
('61-100', '0-30', '0-30', '0-30', '0-30', 'BIF가 매우 안정적인 감정 상태를 유지하고 있습니다. 이런 균형잡힌 상태는 건강한 정신 상태를 나타냅니다.'),
('0-30', '61-100', '0-30', '0-30', '0-30', 'BIF가 매우 긍정적인 감정을 많이 느끼고 있습니다! 이런 좋은 기분을 유지할 수 있도록 지지해주세요.');

-- 테스트용 emotion_statistics 데이터 삽입 (다양한 시나리오)
INSERT INTO emotion_statistics (bif_id, year_value, month_value, emotion_statistics_text, guardian_advice_text, emotion_counts, top_keywords, created_at, updated_at) VALUES
-- BIF ID 1: 긍정적 감정이 많은 경우
(1, 2025, 1, '이번 달은 좋은 감정이 가장 많이 나타났습니다. 전반적으로 긍정적인 기분을 유지하셨네요. 이런 좋은 감정이 계속 이어지길 바랍니다.', 'BIF가 매우 긍정적인 감정을 많이 느끼고 있습니다. 이런 좋은 기분을 유지할 수 있도록 지지해주세요. 함께 즐거운 활동을 해보는 것도 좋겠어요.', '{"OKAY":2, "GOOD":8, "ANGRY":1, "DOWN":1, "GREAT":3}', '[{"keyword":"친구", "count":15}, {"keyword":"게임", "count":12}, {"keyword":"학교", "count":10}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BIF ID 2: 평범한 감정이 많은 경우
(2, 2025, 1, '이번 달은 평범한 감정이 가장 많았습니다. 안정적이고 일상적인 한 달을 보내신 것 같아요. 이런 안정감은 건강한 정신 상태를 나타냅니다.', 'BIF가 안정적인 감정 상태를 유지하고 있습니다. 이런 균형잡힌 상태는 건강한 정신 상태를 나타냅니다. 현재의 안정감을 지지해주세요.', '{"OKAY":10, "GOOD":3, "ANGRY":1, "DOWN":1, "GREAT":1}', '[{"keyword":"학교", "count":20}, {"keyword":"숙제", "count":8}, {"keyword":"가족", "count":5}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BIF ID 3: 부정적 감정이 많은 경우
(3, 2025, 1, '이번 달에는 화난 감정이 많이 나타났습니다. 스트레스가 많았거나 힘든 일이 있었나요? 이런 감정은 자연스러운 반응이니 너무 걱정하지 마세요.', 'BIF가 화난 감정을 많이 경험하고 있습니다. 스트레스나 불만이 있을 수 있어요. 따뜻한 관심과 대화를 통해 원인을 파악해보세요.', '{"OKAY":2, "GOOD":1, "ANGRY":8, "DOWN":3, "GREAT":1}', '[{"keyword":"스트레스", "count":12}, {"keyword":"화남", "count":8}, {"keyword":"힘들어", "count":6}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BIF ID 1의 전월 데이터 (월별 변화 테스트용)
(1, 2024, 12, '지난달에는 평범한 감정이 가장 많았습니다. 안정적인 한 달을 보내셨네요.', 'BIF가 안정적인 감정 상태를 유지하고 있었습니다. 이런 균형잡힌 상태를 지지해주세요.', '{"OKAY":8, "GOOD":3, "ANGRY":2, "DOWN":1, "GREAT":1}', '[{"keyword":"학교", "count":12}, {"keyword":"친구", "count":8}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BIF ID 2의 전월 데이터
(2, 2024, 12, '지난달에는 좋은 감정이 가장 많았습니다. 행복한 한 달을 보내셨네요.', 'BIF가 긍정적인 감정을 많이 느끼고 있었습니다. 이런 좋은 기분을 지지해주세요.', '{"OKAY":3, "GOOD":8, "ANGRY":1, "DOWN":1, "GREAT":2}', '[{"keyword":"친구", "count":15}, {"keyword":"게임", "count":10}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- BIF ID 3의 전월 데이터
(3, 2024, 12, '지난달에는 평범한 감정이 가장 많았습니다. 안정적인 한 달을 보내셨네요.', 'BIF가 안정적인 감정 상태를 유지하고 있었습니다. 이런 균형잡힌 상태를 지지해주세요.', '{"OKAY":7, "GOOD":4, "ANGRY":2, "DOWN":1, "GREAT":1}', '[{"keyword":"학교", "count":10}, {"keyword":"친구", "count":6}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); 