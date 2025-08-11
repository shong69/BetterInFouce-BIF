-- 개발용 초기 데이터
-- 소셜 로그인 2명 (BIF, GUARDIAN)
INSERT INTO social_login (email, provider, provider_unique_id, created_at, updated_at)
VALUES
  ('bif1@example.com', 'GOOGLE', 'google-bif-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('guardian1@example.com', 'KAKAO', 'kakao-guardian-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- BIF 사용자 1명 (social_id = 1 가정)
INSERT INTO bif (social_id, nickname, connection_code, created_at, updated_at)
VALUES (1, 'bifUser1', 'ABC123', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Guardian 사용자 1명 (social_id = 2, bif_id = 1)
INSERT INTO guardian (social_id, bif_id, nickname, created_at, updated_at)
VALUES (2, 1, 'guardianUser1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 일기 3건 (bif_id = 1)
-- selected_emotion은 enum ordinal(0~4): EXCELLENT(0), JOY(1), NEUTRAL(2), SAD(3), ANGER(4)
INSERT INTO emotion_diary (bif_id, selected_emotion, content, created_at, updated_at, is_deleted)
VALUES
  (1, 1, '오늘은 정말 즐거웠어!', CURRENT_TIMESTAMP - 3, CURRENT_TIMESTAMP - 3, FALSE), -- JOY
  (1, 2, '그냥 그랬던 하루', CURRENT_TIMESTAMP - 2, CURRENT_TIMESTAMP - 2, FALSE),       -- NEUTRAL
  (1, 3, '조금 우울했어', CURRENT_TIMESTAMP - 1, CURRENT_TIMESTAMP - 1, FALSE);         -- SAD

-- 각 일기에 대한 AI 피드백 (단순 더미)
INSERT INTO emotion_feedback (diary_id, content, content_flagged, content_flagged_categories, created_at)
VALUES
  (1, '좋은 하루였네요! 계속 유지해봐요.', FALSE, NULL, CURRENT_TIMESTAMP - 3),
  (2, '보통의 하루도 소중합니다.', FALSE, NULL, CURRENT_TIMESTAMP - 2),
  (3, '자기 돌봄이 필요해 보여요. 가벼운 산책은 어때요?', FALSE, NULL, CURRENT_TIMESTAMP - 1);

-- 통계 더미 (emotion_statistics 테이블 - Stats 엔티티와 매핑)
INSERT INTO emotion_statistics (bif_id, year_value, month_value, emotion_statistics_text, guardian_advice_text, emotion_counts, top_keywords, created_at, updated_at)
VALUES
  (1, 2025, 8, '이 달은 전반적으로 긍정적인 감정이 많았습니다.', '규칙적인 수면과 식사로 컨디션을 유지하도록 도와주세요.', '{"OKAY": 10, "GOOD": 5, "DOWN": 3, "ANGRY": 1, "GREAT": 4}', '[{"keyword":"학교","count":5},{"keyword":"친구","count":4},{"keyword":"숙제","count":3}]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 템플릿 더미
INSERT INTO emotion_statistics_template (okay_range, good_range, angry_range, down_range, great_range, statistics_text)
VALUES ('0-20%', '20-40%', '0-10%', '0-10%', '40-60%', '감정 분포가 안정적입니다.');

INSERT INTO guardian_advice_template (okay_range, good_range, angry_range, down_range, great_range, advice_text)
VALUES ('0-20%', '20-40%', '0-10%', '0-10%', '40-60%', '칭찬과 격려를 자주 해주세요.');

-- 투두, 시뮬레이션 더미
INSERT INTO todos DEFAULT VALUES;
INSERT INTO simulations DEFAULT VALUES;

-- 로그인 로그 더미
INSERT INTO user_login_logs (social_id, login_at)
VALUES (1, CURRENT_TIMESTAMP - 1);
