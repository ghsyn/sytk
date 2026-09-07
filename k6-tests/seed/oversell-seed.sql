-- 핫스팟 oversell 테스트용 시드
-- 좌석은 애플리케이션 로직상 CLOSED 로만 생성되고 AVAILABLE 로 여는 API 가 없으므로,
-- 테스트 대상 좌석 1석을 DB 에 직접 AVAILABLE 상태로 심는다.
-- 실행: psql "postgresql://postgres:postgres@localhost:5432/sytk_dev" -f k6-tests/seed/oversell-seed.sql
-- 반복 실행 가능하도록 관련 테이블을 초기화(ID 리셋)한 뒤 seat.id = 1 을 보장한다.

TRUNCATE TABLE reservation, seat, seat_grade, concert RESTART IDENTITY CASCADE;

INSERT INTO concert (title, start_at, venue)
VALUES ('oversell-hotspot', now() + interval '30 days', 'k6-load-test-venue');   -- id = 1

INSERT INTO seat_grade (name, price, total_seat_count, concert_id)
VALUES ('VIP', 100000.00, 1, 1);   -- id = 1

INSERT INTO seat (number, status, seat_grade_id)
VALUES (1, 'AVAILABLE', 1);   -- id = 1  ← k6 의 SEAT_ID 기본값과 일치
