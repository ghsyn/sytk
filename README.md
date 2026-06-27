# SYTK — Service Your Ticket

> 대규모 트래픽 환경을 고려한 티켓 예매 서비스  
> 2026.XX ~ 2026.XX (개인 프로젝트)  
> Modular Monolith · CQRS · Event-Driven Architecture · 다중 전략 동시성 제어

### 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [모듈 구성](#4-모듈-구성)
5. [핵심 설계 결정](#5-핵심-설계-결정)
6. [비즈니스 흐름](#6-비즈니스-흐름)
7. [프로젝트 구조 및 실행](#7-프로젝트-구조-및-실행)
8. [도전 과제, 트러블 슈팅, 성과](#8-도전-과제,-트러블-슈팅,-성과)

---

## 1. 프로젝트 개요

티켓팅 서비스는 예매 오픈 직후 찰나의 순간에 수만 명의 트래픽이 몰리는 극단적인 특수성을 가집니다.  
SYTK는 이 문제에 정면으로 대응하는 것을 목표로 설계했습니다.

**핵심 기술 과제**

| 문제 | 대응 전략 |
|---|---|
| 동일 좌석 동시 선점 (중복 예매, 이선좌) | 도메인 상태 머신 기반 원자적 처리 + 상황별 락 전략 분리 |
| 예매 오픈 시 DB 병목 | Redis Sorted Set 기반 대기열로 진입 트래픽 제어 |
| 조회 트래픽이 예매 성능에 영향 | CQRS로 Command/Query 물리적 분리, Read-Replica 라우팅 |
| 결제 실패 시 선점 좌석 미회수 | Outbox 패턴 + 스케줄러 이중 방어 |
| 서비스 간 결합으로 인한 연쇄 장애 | Kafka 비동기 이벤트 기반 통신, 모듈 간 직접 참조 금지 |

---

## 2. 기술 스택

| 분류 | 기술 | 선정 이유 |
|---|---|---|
| Language / Framework | Java 17, Spring Boot 3.5 | Record, Sealed Class 등 최신 언어 기능 활용 + 생태계 안정성 |
| ORM / Query | Spring Data JPA, QueryDSL | 복잡한 동적 쿼리를 타입 안전하게 작성 |
| Database | PostgreSQL (Master / Replica) | ACID 보장 + Replica로 읽기 부하 분산 |
| Cache / Message | Redis (Sorted Set · Cache), Kafka | 대기열·캐시는 Redis, 모듈 간 이벤트 전파는 Kafka로 역할 분리 |
| Concurrency | Redisson · JPA `@Version` · `SELECT FOR UPDATE` | 충돌 특성별 최적 락 전략 적용 ([5-5 참고](##5-5-상황별-동시성-제어-전략)) |
| Test | JUnit 5, AssertJ, BDDMockito, Testcontainers | 실제 DB/Redis 컨테이너 기반 동시성 통합 테스트 |
| Documentation | Spring REST Docs | 테스트 통과 시에만 문서 생성 (코드와 문서 간 항상 일치 보장) |
| Build / Infra | Gradle Multi-Module, Docker Compose, GitHub Actions | 모듈별 독립 빌드 + CI/CD 자동화 |

---

## 3. 시스템 아키텍처

```
                         ┌─────────────────────────────────────────┐
                         │            Client (Browser)             │
                         └──────────────────┬──────────────────────┘
                                            │
                         ┌──────────────────▼──────────────────────┐
                         │         sytk-waiting  :8083             │
                         │   Redis Sorted Set 기반 대기열 / 토큰 발급   │
                         └──────────────────┬──────────────────────┘
                                            │ WaitingToken (통과 시)
               ┌────────────────────────────┼──────────────────┐
               │                            │                  │
  ┌────────────▼──────────┐    ┌────────────▼──────────┐       │
  │   sytk-read  :8082    │    │  sytk-booking  :8080  │       │
  │  Query 전담 · Redis캐시 │    │  Command 전담 · 분산 락  │       │
  │  PostgreSQL Replica   │    │  PostgreSQL Master    │       │
  └───────────────────────┘    └────────────┬──────────┘       │
                                            │ Kafka Event      │
                               ┌────────────▼──────────┐       │
                               │  sytk-payment  :8081  │       │
                               │  Mock PG · Outbox 패턴 │       │
                               └───────────────────────┘       │
                                                               │
  ← sytk-read는 Kafka 이벤트를 구독해 캐시를 비동기로 동기화 ────────────┘
```

**모듈 간 통신 원칙**
- HTTP / FeignClient 등 동기 호출 전면 금지
  - 모든 도메인 간 데이터 전파는 Kafka 비동기 이벤트로만 처리
- Event DTO는 각 모듈이 독립 정의 (공통 모듈 없음, [5-2 참고](#5-2-공통-모듈-제거))

---

## 4. 모듈 구성

### 1) sytk-booking — _비즈니스의 심장_

티켓팅 핵심 로직인 좌석 선점과 예매 상태 관리를 담당합니다.  
**충돌 특성에 따라 락 전략을 3가지로 분리**했습니다. ([5-5 참고](#5-5-상황별-동시성-제어-전략))

| 상황 | 락 전략 | 선택 이유 |
|---|---|---|
| 좌석 선점 | Redisson 분산 락 | "초고경쟁 트래픽"<br>DB가 아닌 Redis 메모리에서 선점권 결정, DB 부하 원천 차단 |
| 예매 만료 처리 | 낙관적 락 (`@Version`) | "충돌 빈도 낮음"<br>만료 스케줄러 vs 결제 완료의 약한 충돌, 경량 처리로 충분 |
| 결제 처리 | 비관적 락 (`SELECT FOR UPDATE`) | "반드시 순차 처리"<br>중복 결제 원천 차단, 멱등성 보장 |

- Redisson 분산 락으로 동일 좌석 동시 선점 원천 차단
- 좌석 상태 머신 `CLOSED → AVAILABLE → OCCUPIED → SOLD` 도메인 수준에서 강제
- 미결제 선점 좌석은 스케줄러가 주기적으로 회수 (2차 방어)
- CQRS Command side 담당 (조회 로직 작성 금지)

**좌석 상태 전이**
```
CLOSED ←→ AVAILABLE → OCCUPIED → SOLD
               ↑       ↓
               AVAILABLE (취소/만료)
```
상태 전이는 반드시 `SeatStatus.canChangeTo()` 검증 후 `Seat.changeStatus()`로만 위임됩니다.

### 2) sytk-payment — _신뢰할 수 있는 결제와 복구_

외부 PG사 연동 및 결제 후처리를 격리한 모듈입니다.  
결제 지연이나 실패가 예매 도메인에 파급되지 않도록 분리했습니다.

- Mock PG 어댑터로 성공/실패/타임아웃 시나리오 처리
- **Transactional Outbox 패턴**: 결제 이벤트를 outbox 테이블에 먼저 저장 후 Kafka로 릴레이 → 메시지 유실 방지 (At-least-once 보장)
- 결제 실패 시 `SeatReleaseEvent` 발행 → sytk-booking이 즉시 좌석 복구

### 3) sytk-read — _구경꾼을 위한 고속도로_

조회 트래픽이 예매 성능에 영향을 주지 않도록 물리적으로 분리된 모듈입니다.

- PostgreSQL Read-Replica + Redis Cache 이중 구조로 조회 성능 최적화
- `AbstractRoutingDataSource`로 읽기 요청을 Replica로 자동 라우팅
- sytk-booking 이벤트를 Kafka로 구독해 캐시 비동기 업데이트 — 상태 변경(CUD) 로직 작성 엄격 금지

### 4) sytk-waiting — _시스템의 최전방 방어선_

예매 오픈 시 유입되는 모든 트래픽을 Redis Sorted Set으로 흡수합니다.  
유효한 `WaitingToken`을 발급받은 사용자만 예매 시스템 진입을 허용해 DB 과부하를 원천 차단합니다.

- Redis Sorted Set으로 사용자 진입 순번 관리 (score = 요청 timestamp)
- 처리 가능 인원만큼만 통과시키는 TPS 제어 (Throttling)
- 향후 트래픽 증가 시 가장 먼저 독립 서비스로 분리 가능한 구조

---

## 5. 핵심 설계 결정

### 5-1. Modular Monolith
_추진력과 확장성 동시 확보_

처음부터 MSA로 시작하면 네트워크 지연, 분산 트랜잭션 등 관리 포인트만 늘어납니다.
단일 JVM에서 동작하되, 도메인 경계를 명확히 나눠 **필요한 시점에 특정 모듈만 MSA로 분리**할 수 있는 구조를 선택했습니다.

> 예: 트래픽이 폭증하면 sytk-waiting만 독립 서비스로 떼어내고 앞단에 Kafka를 붙이면 됩니다.

### 5-2. 공통 모듈 제거
_중복보다 무서운 덩어리 관리_

공통 모듈(`Core`/`Common`)이 커질수록 모든 모듈이 불필요한 의존성을 가져가며 빌드 효율이 떨어집니다.  
**일부 DTO 중복을 허용하는 대신**, 모듈 간 결합을 완전히 제거했습니다.
각 모듈은 필요한 엔티티/DTO를 자신의 컨텍스트에 맞게 독립 정의합니다.

### 5-3. Rich Domain Model
_Service가 아닌 도메인이 동작_

`Anemic Domain Model`을 경계하고 비즈니스 로직을 도메인 엔티티 내부에 응집시켰습니다.  
Service 계층은 `조회 → 도메인에 명령 → 저장`의 흐름 제어(Orchestration)만 담당합니다.

```java
// ❌ Service가 도메인 로직을 직접 처리
if (seat.getStatus() == AVAILABLE) seat.setStatus(OCCUPIED);

// ✅ 도메인이 스스로 상태를 검증하고 변경
seat.hold();  // 내부에서 AVAILABLE 여부 검증 후 OCCUPIED로 전이
```

### 5-4. CQRS
_조회가 예매를 방해하지 않도록_

티켓팅에서 실제 조회 트래픽은 예매 트래픽의 수십 배에 달해 서버 과부하의 주된 원인이 됩니다.  
같은 DB를 공유한다면 조회 폭주 시 예매 커넥션 풀이 잠식됩니다.

- **Command (sytk-booking)**: PostgreSQL Master, 강한 정합성 보장
- **Query (sytk-read)**: Redis Cache + Read-Replica, 빠른 응답 우선

두 모델 간 정합성은 Kafka 이벤트로 **최종 정합성(Eventual Consistency)** 방식으로 유지합니다.

### 5-5. 상황별 동시성 제어 전략
_상황에 맞는 락 전략 고려_

동시성 제어는 "가장 강한 락"이 정답이 아닙니다.  
충돌 빈도, 실패 비용, 트래픽 규모에 따라 락 전략을 달리 적용했습니다.

#### 🔷 좌석 예매

> **상황** | 예매 오픈 순간 수천 명이 동시에 동일 좌석에 대해 예매를 시도합니다.  
> **특징** | 초고경쟁 부하 / 매우 빠른 속도로 이루어짐, 절대 중간에 실패하면 안 됨

- [ ] 낙관적 락 - 불가
  - `version` 충돌로 1건 성공 시 나머지 전부 롤백 
  - 예) 1,000명 경쟁 시, 1회차: 성공 1 · 실패 999 / 2회차: 성공 1 · 실패 998 … → 재시도 횟수 기하급수적으로 쌓임, 성능 저하 유발
- [ ] 비관적 락 - 불가
  - 선점한 행을 DB 수준에서 잠가버리면 이후 사용자 전원이 결제 완료까지 대기해야 함
  - DB 커넥션이 고갈되고 최악의 경우 스레드 고갈, 서버 다운
- [x] **분산 락 (Redisson)**
  - 선점 경쟁을 DB가 아닌 **Redis 메모리에서 처리**
  - `SETNX` 기반으로 단 하나의 요청만 락 획득, 나머지는 DB에 도달하기 전 즉시 실패 응답 받을 수 있음
  - DB 커넥션을 소모하지 않아 예매 폭주 상황에서도 시스템이 보호됨

    ```
    요청: 1,000건 동시 진입
             │
        [Redis 분산 락]  ← DB 도달 전 필터링
        락 획득: 1건만 통과
        락 실패: 999건 즉시 반환 (DB 부하 없음)
             │
        [PostgreSQL] ← 단 1건만 도달
        좌석 OCCUPIED 처리
    ```

#### 🔷 예매 만료

> **상황** | 결제 타임아웃이 임박 시 "만료 스케줄러" vs "결제 완료 처리" 동시에 같은 예매 레코드를 건드릴 수 있습니다.  
> **특징** | 약한 충돌 / 부하가 가볍고 충돌 가능성이 희박, 실패 시 재시도 가능

- [ ] 비관적 락 - 불필요 
  - 비관적 락을 걸면 전혀 관련 없는 다른 결제 요청까지 대기하게 되어 리소스 낭비됨
- [ ] 분산 락 - 과잉
  - 분산락은 Redis라는 외부 시스템에 락을 걸어야 해서 네트워크 왕복 비용이 발생함
  - 외부 Redis 호출 비용을 지불할 만큼 충돌 빈도가 높지 않아 오버 엔지니어링
- [x] **낙관적 락 (`@Version`)**
  - `@Version` 컬럼 하나로 "내가 읽었을 때의 상태 그대로인가"를 DB 커밋 시점에 검증
  - 만료와 결제 완료 중 선착순 1건만 성공, 나머지는 `ObjectOptimisticLockingFailureException`으로 우아한 실패

#### 🔷 결제 처리

> **상황** | 결제 요청부터 완료까지의 흐름  
> **특징** | 원자성 보장 / 다른 요청으로부터 원천 봉쇄해야함, 돈이 나간 이후 롤백 절대 불가

- 반드시 순차 처리
  - 결제 버튼 클릭 → 요청 → PG사 승인 → DB 커밋 → 완료 처리 의 흐름이 원자적으로 보장되어야 함
- 중복 결제 원천 차단
  - 동일 예매에 대한 결제 요청이 동시에 들어오면 결제 대상 레코드를 트랜잭션 종료까지 완전히 잠가서 타 요청은 현재 요청이 끝날 때까지 완전히 대기시킴
  - 낙관적 락처럼 실패 후 재시도하는 구조는 이중 과금 위험
- **비관적 락 (`SELECT FOR UPDATE`)**
  - 결제 처리는 건당 요청이므로 커넥션 경합 부담이 없음
  - 멱등성 보장이 데이터 정확성보다 우선임

### 5-6. 이벤트 기반 보상 트랜잭션
_좌석이 영영 묶이지 않도록_

단순 try-catch로 복구 로직을 짜면 서버가 꺼지는 순간 선점 좌석이 영영 풀리지 않습니다.  
결제 실패 이벤트와 스케줄러를 조합한 2중 방어로 해당 문제를 해결합니다.

**2중 방어 전략**

1. **이벤트 기반**: 결제 실패/타임아웃 발생 → `SeatReleaseEvent` 발행 → sytk-booking 즉시 좌석 해제
2. **스케줄러**: 만료 시간이 지난 OCCUPIED 좌석을 주기적으로 스캔해 강제 회수

### 5-7. 데이터 모델

> ERD 추가 예정 
> 데이터베이스 구조를 보여주고, 핵심 엔티티 간의 관계나 설계 시 고려한 점을 서술
> 핵심 설계 포인트: (예: 대용량 데이터를 고려한 인덱스 설계, 정규화 및 반정규화 이유 등)

---

## 6. 비즈니스 흐름

**티켓팅 플로우**
```
   Client
     ↓
[sytk-waiting]      1. 대기열 합류 → Redis Sorted Set에 순번 등록 → WaitingToken 수신
     ↓ (순번 도달 시 토큰 유효)
[sytk-read]         2. 공연 목록 / 좌석 현황 조회 (Redis Cache → Replica DB)
     ↓ (좌석 선택)
[sytk-booking]      3. 분산 락 획득 → 좌석 OCCUPIED → Reservation(PENDING) 생성
     ↓ (결제 창 오픈, 10분 타임아웃)
[sytk-payment]      4. PG 결제 처리 → 성공 시 PaymentCompletedEvent 발행
     ↓
[sytk-booking]      5. 이벤트 소비 → Reservation CONFIRMED → 좌석 SOLD
     ↓
[sytk-read]         6. ReservationConfirmedEvent 소비 → 잔여 좌석 캐시 업데이트
```

**실패 시나리오**
- 결제 타임아웃: `sytk-payment` → `SeatReleaseEvent` → `sytk-booking` 좌석 AVAILABLE 복구
- 서버 장애로 이벤트 미발행: 스케줄러가 만료된 OCCUPIED 좌석 자동 회수

---

## 7. 프로젝트 구조 및 실행

**프로젝트 구조 요약**

```
sytk/
├── sytk-{module}/                              # 4가지 멀티 모듈
│       ├── src/main/resources/
│       │       ├── application.yml             # 공통 설정
│       │       ├── application-dev.yml         # 개발 환경 설정
│       │       └── application-prod.yml        # 운영 환경 설정
│       ├── build.gradle                        # 모듈별 의존성 관리
│       └── Dockerfile                          # 모듈별 컨테이너 이미지 빌드
├── docker-compose-dev.yml                      # 로컬 인프라 (PostgreSQL, Redis, Kafka)
└── build.gradle                                # 루트 의존성 관리
```

**기본 접속 정보**

| 항목 | 값 |
|---|---|
| PostgreSQL | `localhost:5432` / `postgres` / `postgres` |
| Database | `sytk_dev` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |

**로컬 실행** (Java 17, Docker 필요)

```bash
# 1. 저장소 클론
git clone https://github.com/your-username/sytk.git
cd sytk

# 2. 로컬 인프라 실행 (PostgreSQL:5432 · Redis:6379 · Kafka:9092)
docker compose -f docker-compose-dev.yml --profile infra up -d

# 3. 전체 빌드
./gradlew build

# 4. 각 모듈 실행 (포트: booking=8080, payment=8081, read=8082, waiting=8083)
./gradlew :sytk-booking:bootRun
./gradlew :sytk-read:bootRun
./gradlew :sytk-payment:bootRun
./gradlew :sytk-waiting:bootRun
```

**테스트 및 API 문서**

```bash
./gradlew test                          # 전체 테스트
./gradlew :sytk-booking:test            # 모듈별 테스트

./gradlew :sytk-booking:asciidoctor     # REST Docs 생성
# 생성 경로: sytk-booking/build/resources/main/static/docs/index.html
```

**테스트 전략**

| 구분 | 도구 | 대상 |
|---|---|---|
| 단위 테스트 | JUnit 5 + AssertJ + BDDMockito | 도메인 상태 머신, 비즈니스 규칙 |
| 컨트롤러 테스트 | @WebMvcTest + Spring REST Docs | API 입력 검증, 응답 포맷 |
| 동시성 통합 테스트 | Testcontainers | 1000명 동시 좌석 선점 시나리오 |

---

## 8. 도전 과제, 트러블 슈팅, 성과
개인 도전 과제 & 성과 요약 추가 예정

> 예시)
> * **성과 지표**: 테스트 코드 커버리지 X% 달성, API 응답 시간 평균 X% 단성 등