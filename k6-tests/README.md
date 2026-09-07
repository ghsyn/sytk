# k6 부하 · 동시성 테스트

`sytk-booking` 모듈의 동시성 제어 및 성능을 검증하는 k6 테스트 모음.

> 이 문서 하나에 **모든 시나리오의 실행 절차와 결과**를 기록한다.
> 시나리오가 많아지면 각 섹션을 `scenarios/<name>/README.md`로 분리한다.

---

## 디렉터리 구조

```
k6-tests/
├── README.md                       # (현재 문서) 실행 절차 + 결과 기록
├── seed/
│   └── oversell-seed.sql           # 핫스팟 테스트용 AVAILABLE 좌석 시드
└── scenarios/
    └── booking/
        └── concurrency.js          # 동시 예매 초과판매(oversell) 검증
```

---

## 공통 사전 준비

### 1. 좌석 시드가 필요한 이유

좌석은 애플리케이션 로직상 `CLOSED` 상태로만 생성되고, `CLOSED → AVAILABLE`로 여는 API 스케줄러가 아직 구현되지 않았다.  
예매(`reserve`)는 좌석이 `AVAILABLE`이어야만 성공하므로, 테스트 대상 좌석을 **DB에 직접 `AVAILABLE`로 심어야 한다.**  
`seed/oversell-seed.sql`이 그 역할을 하며, 매 실행 전 테이블을 초기화하고 `seat.id = 1`을 재생성한다.

### 2. 실행 방식 선택

| 방식 | 앱 실행 위치 | k6 → 앱 연결 |
|---|---|---|
| **A. 전부 도커** | compose (`app` 프로파일) | `http://booking-service:8080` (기본값, 자동) |
| **B. 앱만 호스트** | gradle `bootRun` | `http://host.docker.internal:8080` (오버라이드) |

- `infra`는 항상 `docker`로 실행한다.  
- 개발 단계에서는 주로 **B 방식**을 사용한다.  
- `dev` 프로파일은 `ddl-auto: update`이므로 **앱이 먼저 떠야 테이블이 생성**된다.  
- 따라서 실행 순서는 항상 **앱 기동 → 시드 → k6** 이다.

#### A. 전부 도커

```bash
# 1) 인프라 + 앱 기동
docker compose -f docker-compose-dev.yml --profile infra --profile app up -d

# 2) booking 앱이 완전히 뜰 때까지 대기 (테이블 생성 확인)
docker compose -f docker-compose-dev.yml logs -f booking-service   # "Started ...Application" 뜨면 Ctrl+C

# 3) 시드 주입 (AVAILABLE 좌석 1석)
docker exec -i sytk-postgres psql -U postgres -d sytk_dev < k6-tests/seed/oversell-seed.sql

# 4) k6 실행
docker compose -f docker-compose-dev.yml --profile test run --rm k6
```

#### B. 앱만 호스트

```bash
# 1) 인프라만 기동
docker compose -f docker-compose-dev.yml --profile infra up -d

# 2) booking 앱 실행 (별도 터미널)
./gradlew :sytk-booking:bootRun --args='--spring.profiles.active=dev'

# 3) 시드 주입
docker exec -i sytk-postgres psql -U postgres -d sytk_dev < k6-tests/seed/oversell-seed.sql

# 4) k6 실행 (호스트 앱을 가리키도록 오버라이드)
K6_BASE_URL=http://host.docker.internal:8080 docker compose -f docker-compose-dev.yml --profile test run --rm k6
```

---

## 시나리오 1. 동시 예매 초과판매 (`scenarios/booking/concurrency.js`)

### 목적
동시성 제어가 없는 현재 상태에서, 좌석 1석(핫스팟)에 대량 동시 예매를 발생시켜
**초과판매(oversell)가 실제로 발생함을 증명**한다. (Before: 통과가 아닌 실패 관찰이 목표)

### 부하 설정
- executor: `shared-iterations`
- VUs: `500`, iterations: `1000`, maxDuration: `30s`
- 대상: `seat.id = 1` 고정(핫스팟), `userId`는 VU별로 분산

### 결과 읽는 법
- **`reservation_created`** — 핵심 지표. `1`이면 정상, **`2` 이상이면 초과판매 발생**(동시성 제어 부재 증명).
- **`checks`** — `created (201)` / `conflict (409)` 비율로 성공·충돌 분포 확인.
- **`http_req_duration`** — 요청 처리 시간(전체 기준).

### 측정 지표 범위
| 지표 | 측정 위치 | 비고 |
|---|---|---|
| Oversell count | ✅ k6 (`reservation_created`) | 201이 2건 이상이면 초과판매 |
| 성공 요청 처리 시간 | △ k6 (`http_req_duration`, 전체) | 성공만 분리하려면 커스텀 Trend 필요 |
| Lost update | ❌ DB 직접 조회 | 클라이언트로 판별 불가 (좌석 상태 vs 예매 건수 대조) |
| Deadlock / Rollback율 | ❌ 서버 로그 / `pg_stat_database.xact_rollback` | k6가 볼 수 없음 |

### 실행 방법론
- **워밍업 1회(결과 버림)** 후 **측정 3회** → 중앙값/범위로 기록.
- **매 실행 전 시드(3번)를 다시 주입**한다. 안 하면 좌석이 이미 팔려 두 번째 run부터 전부 409.

### 결과 기록 — Before (동시성 제어 없음)

측정 환경: Docker Desktop / PostgreSQL 15 / booking dev 프로파일 (앱은 호스트 gradle, k6는 도커) / 2026-09-07  
설정: 좌석 1석에 500 VU · 1000 iterations · HikariCP `maximum-pool-size: 50`

| 회차 | reservation_created (성공 201) | 409 conflict | http_req_duration p95 | 성공 요청 p95(expected_response) | 소요 | 초과판매 |
|---|---|---|---|---|---|---|
| 워밍업(1) | (버림) | 950 | 6.08s | 9.88s | 10.6s | — |
| 1 | **50** | 950 | 5.30s | 7.92s | 8.7s | **O** |
| 2 | **49** | 951 | 3.02s | 5.64s | 6.3s | **O** |
| 3 | **36** | 964 | 2.36s | 4.41s | 4.6s | **O** |

**요약:**
- 좌석 1석에 대해 **성공(201)이 3회 모두 36~50건** → 1건이어야 정상이므로 **초과판매 재현**. (측정 3회 중앙값 49, 범위 36~50)
- oversell 규모가 **커넥션 풀 크기(50) 부근**에서 관찰됨: 락이 없고 격리수준이 Read Committed라, 동시에 열린 트랜잭션들이 좌석을 모두 `AVAILABLE`로 읽은 뒤 각자 `seat.hold()`  
  → 전부 커밋되어 예매가 생성됨. 첫 커밋 이후 좌석이 `OCCUPIED`가 되면서 나머지는 `409`. **즉 초과판매 규모 ∝ 동시 트랜잭션 수 = 풀 크기.**
- 정확히 50이 아닌 이유: read→commit 구간에서 실제로 겹친 트랜잭션 수에 좌우됨. 회차가 진행되며 커밋이 빨라질수록(소요 10.6s→4.6s) 겹침 구간이 좁아져 마지막 회차는 36까지 감소.
- `http_req_failed 95%`는 문제가 아니라 k6가 409를 실패로 집계한 것(예상된 충돌).
- **결론: 좌석 선점에 동시성 제어(Redisson 분산락)가 반드시 필요.** 현 상태는 커넥션 풀 크기에 비례해 좌석이 중복 판매됨.

### 결과 기록 — After (분산락 적용 후)

> 분산락 적용 후 동일 시나리오 재실행. `reservation_created`가 `1`로 수렴하는지 확인하여 Before와 비교한다.

| 회차 | reservation_created (성공 201) | 409 conflict | http_req_duration p95 | 성공 요청 p95 | 소요 | 초과판매 |
|---|---|---|---|---|---|---|
| 워밍업 | (버림) | | | | | |
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |

**요약:**
- (분산락 적용 후 결과와 Before 대비 결론을 적는다)

---

## 시나리오 2. (예: 여러 좌석 분산 — 순수 TPS 측정)

> 아직 미작성. 추가 시 위와 동일한 구조(목적 / 부하 설정 / 실행 / 결과 기록)로 이 아래에 이어서 작성한다.
