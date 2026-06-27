# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. It enforces the project's architectural constraints, coding standards, and development workflows.


## 프로젝트 개요

`sytk`는 공연 티켓팅 시스템으로, Gradle 멀티 모듈 구조의 Spring Boot 3.5 / Java 17 기반 **Modular Monolith**입니다. 단일 JVM에서 동작하되 도메인 경계를 명확히 분리하여, 필요 시 특정 모듈만 독립 서비스로 분리할 수 있는 구조입니다.

| 모듈 | 포트 | 역할                              | 주요 인프라 |
|---|---|---|---|
| `sytk-booking` | 8080 | 공연/좌석/예약 관리 (CQRS Command side) | PostgreSQL, Redis(Redisson 분산락), Kafka, QueryDSL |
| `sytk-payment` | 8081 | 결제 처리                           | PostgreSQL, Kafka |
| `sytk-read` | 8082 | 조회 전용 서비스 (CQRS Read side)      | PostgreSQL(Replica 복제본), Redis(캐시), Kafka |
| `sytk-waiting` | 8083 | 대기열 관리                          | Redis(Sorted Set), Kafka |

---

## 아키텍처 핵심 원칙 & 제약 사항

> [!IMPORTANT]
> Claude는 코드를 생성하거나 리팩토링할 때 아래의 아키텍처 제약 사항을 엄격히 준수해야 합니다.

### 1. Java & General Standards
- **Record 사용**: DTO, API 요청/응답 바디, 단순 데이터 캐리어 역할을 하는 객체는 되도록 클래스가 아닌 `record`로 작성합니다.
- **Builder Pattern**: `@NoArgsConstructor(access = PROTECTED)` + `@Builder` 조합을 사용합니다. (생성자 파라미터가 3개 이상인 경우엔 특히 필수) 단, 엔티티 내부에서 상태를 변경하는 비즈니스 메서드에는 `..Builder`보다 명확한 명칭의 전용 메서드를 사용해야 합니다.
- **Editor 패턴 (부분 수정)**: PATCH 엔드포인트 등에서 `null` 덮어쓰기를 방지하기 위해 엔티티가 `toEditor()`로 현재 값이 채워진 빌더(Editor)를 반환합니다. 서비스는 변경된 필드만 반영 후 `entity.edit(editor)`를 호출합니다. (`Concert` / `ConcertEditor` 구조 참고)
- **Exception Handling**: 모든 비즈니스 예외는 `CommonException`을 상속받아야 하며, `ErrorCode` enum(`HttpStatus` + 한국어 메시지)을 보유합니다. 전역 예외 핸들러(`GlobalExceptionHandler`)가 이를 공통 `ErrorResponse` 형식으로 변환합니다. 새 에러 코드는 `ErrorCode`에 추가합니다.

### 2. Rich Domain Model (도메인 모델 중심 설계)
- **Fat Service 경계하기 (Anemic Domain Model 방지)**:
    - 비즈니스 로직은 서비스 레이어가 아닌 엔티티 메서드 내부에 위치합니다.
    - Service 계층은 도메인 객체를 조회 ➔ 도메인 객체에 명령(메서드 호출) ➔ 저장으로 이어지는 **흐름 제어(Orchestration)** 역할만 수행합니다.
    - `if`문을 통한 상태 검증이나 복잡한 계산 로직 등 비즈니스 로직은 Service 계층 내부가 아닌 반드시 domain 엔티티 내부 메서드로 응집시켜 구현합니다.
    - ```java
  예시)
  // 올바른 방식: 도메인 메서드가 상태 변경을 소유
  seat.open();    // CLOSED → AVAILABLE
  seat.hold();    // AVAILABLE → OCCUPIED
  seat.sell();    // OCCUPIED → SOLD

  // 잘못된 방식: 서비스가 도메인 로직을 처리
  if (seat.getStatus() == AVAILABLE) seat.setStatus(OCCUPIED);
    ```
- **Setter 사용 금지**:
    - 외부에서 엔티티의 필드를 직접 수정하는 것을 엄격히 금지합니다.
    - 도메인 객체는 항상 유효한 상태를 유지해야 하며, 생성자나 정적 팩토리 메서드, 빌더에서 초기 상태 검증을 수행합니다.
- **캡슐화:**
    - 객체 스스로 자신의 상태를 검증하고 변경합니다.
    - 상태 검증 및 변경은 의미 있게 명명된 비즈니스 메서드(예: `seat.reserve()`, `seat.open()`)로만 처리합니다.
- **생성자 및 빌더 검증**:
    - 도메인 객체는 항상 유효한 상태여야 하므로, 생성자나 정적 팩토리 메서드, 빌더에서 초기 상태 유효성 검증을 수행합니다.

### 3. CQRS 패턴 및 모듈 간 격리 (Fault Tolerance)
- **철저한 CQRS(Read/Command) 분리**: `sytk-booking`(Command)과 `sytk-read`(Read)는 물리적·논리적으로 분리되어 있습니다. `sytk-booking` 모듈 내에서 반드시 필요한 경우가 아니라면 조회 로직을 작성하지 않습니다. `sytk-read` 모듈 내에서는 어떠한 상태 변경(CUD) 로직도 작성해서는 안 되며, 오직 조회 목적의 조회 전용 모델과 데이터 구조만 유지합니다.
- **연쇄 장애 방지**: 특정 도메인의 변경이나 장애가 다른 도메인으로 전파되지 않도록 결합도를 낮춥니다.
- **도메인 간 격리 (Fault Tolerance)**: 특정 도메인의 트래픽 급증 시 해당 도메인만 독립적인 서비스로 분리하거나 고성능 DB로 교체할 수 있도록 도메인 간 참조는 최소화(식별자 참조 권장)합니다.
- **공통 모듈(Core/Common) 없음**: 이벤트 DTO를 포함한 모든 도메인 객체는 각 모듈이 자신의 컨텍스트에 맞게 독립 정의합니다. 모듈 간 결합을 완전히 제거하기 위해 일부 DTO 중복을 허용합니다.

### 4. 비동기 이벤트 기반 통신 (EDA) & 최종 정합성
- **동기식 호출 금지**: 모듈 간 통신 시 HTTP(FeignClient, RestTemplate)나 gRPC를 통한 동기식 API 호출은 절대 금지됩니다.
- **이벤트 전파**: 모든 도메인 간 트랜잭션 전파 및 데이터 싱크는 **Kafka를 통한 비동기 이벤트 발행/소비**로만 처리합니다.
- **최종 정합성(Eventual Consistency)**: Command 모듈과 Read 모듈 간의 데이터는 실시간이 아닌 최종 정합성을 보장합니다. 비즈니스 로직 및 통합 테스트 작성 시 데이터가 동기화되는 지연 시간(e.g., Awaitility 활용)을 고려해야 합니다.
- **Transactional Outbox 패턴**: 결제 이벤트는 outbox 테이블에 먼저 저장 후 Kafka로 릴레이하여 메시지 유실을 방지합니다(At-least-once 보장). `sytk-payment` 모듈이 담당합니다.

### 5. 상황별 동시성 제어 전략

충돌 빈도, 실패 비용, 트래픽 규모에 따라 락 전략을 달리 적용합니다. "가장 강한 락"이 항상 정답이 아닙니다.

| 상황 | 락 전략 | 선택 이유 |
|---|---|---|
| 좌석 선점 | **Redisson 분산 락** | 초고경쟁 트래픽 → DB가 아닌 Redis 메모리에서 선점권 결정, DB 부하 원천 차단 |
| 예매 만료 처리 | **낙관적 락 (`@Version`)** | 충돌 빈도 낮음 → 만료 스케줄러 vs 결제 완료의 약한 충돌, 경량 처리로 충분 |
| 결제 처리 | **비관적 락 (`SELECT FOR UPDATE`)** | 반드시 순차 처리 → 중복 결제 원천 차단, 멱등성 보장 |

---

## 좌석 상태 머신 (`SeatStatus`)

```
CLOSED → AVAILABLE → OCCUPIED → SOLD
               ↑       ↓
            AVAILABLE (취소/만료)
AVAILABLE → CLOSED (미판매)
```

좌석 상태 전이는 비즈니스 핵심 흐름으로, 반드시 `SeatStatus.canChangeTo()` 검증을 거쳐 `Seat.changeStatus()`를 통해 안전하게 위임되어야 합니다.

---

## 빌드 및 테스트 명령어

```bash
# 전체 빌드
./gradlew build

# 전체 테스트 실행
./gradlew test

# 특정 모듈 빌드
./gradlew :sytk-booking:build

# 특정 모듈 테스트
./gradlew :sytk-booking:test

# 단일 테스트 클래스 및 메서드 실행
./gradlew :sytk-booking:test --tests "com.sytk.booking.domain.SeatTest"
./gradlew :sytk-booking:test --tests "com.sytk.booking.domain.SeatTest.메서드명"

# REST Docs 생성 (테스트 스니펫 기반 HTML 생성)
./gradlew :sytk-booking:asciidoctor

# 실행 가능한 JAR 빌드 (REST Docs 포함)
./gradlew :sytk-booking:bootJar
```

---

## 로컬 인프라 (Docker)

```bash
# 개발용 인프라 컨테이너 실행 (PostgreSQL:5432, Redis:6379, Kafka 9092)
docker compose -f docker-compose-dev.yml --profile infra up -d

# (인프라 실행 후) 애플리케이션 컨테이너까지 함께 실행
docker compose -f docker-compose-dev.yml --profile app up -d
```

기본 DB 접속 정보: postgres/postgres @ 데이터베이스명: sytk_dev

---

## 테스트 & 문서화 컨벤션

### 테스트 가이드라인

- **단위 테스트**: JUnit 5 + AssertJ + BDDMockito 구조를 사용합니다.
- **명명 규칙**: 가독성을 위해 모든 테스트 메서드에는 `@DisplayName`을 사용하여 테스트 메서 명을 한국어로 상세하게 기술합니다.
- **컨트롤러 테스트**: `@WebMvcTest` + `@MockitoBean`을 사용하여 서비스 레이어를 모킹합니다.
- **동시성 통합 테스트**: Testcontainers를 사용하여 실제 PostgreSQL/Redis 컨테이너 기반의 동시성 통합 테스트(예: 1,000명 동시 좌석 선점 시나리오)를 작성합니다.

### Spring REST Docs 작성

- `ConcertControllerDocTest` 패턴 (`@WebMvcTest` + `@AutoConfigureRestDocs` + `@ExtendWith(RestDocumentationExtension.class)`)을 따릅니다. 생성된 스니펫 경로는 `build/generated-snippets`입니다.

### API 설계 규칙

- URI 구조: kebab-case 및 복수형 명사를 조합하여 사용합니다. (예: `/api/v1/concerts`)
- 데이터 포맷: 요청/응답 바디는 항상 camelCase JSON 형식을 취합니다.
- HTTP 상태 코드: 리차드슨 성숙도 모델(RMM) Level 2를 준수하여 목적에 맞는 적절한 코드(200, 201, 204, 400, 404, 409, 500)를 응답합니다.
- 인증: JWT 기반의 무상태(Stateless) 인증 체계를 유지하며, 서버는 세션 상태를 관리하지 않습니다.

---

## Git 워크플로우 & 컨벤션

### 1. Branch Strategy

- **GitHub Flow**를 따릅니다: main 브랜치는 항상 배포 가능한 최신 상태여야 하며 직접적인 커밋은 금지됩니다.

### 2. Branch Convention

- **브랜치 명명 규칙**: `{type}/{module-name}/{issue-number}-{description}`  
  type: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`  
  module-name: `root`, `booking`, `waiting`, `read`, `payment`

### 3. Commit Message Convention

- **커밋 메시지 형식**: `Type: 작업 내용 한글로 요약` (50자 이내, 마침표 제거, 현재형/명령문으로 작성, 본문은 필요 시 추가)  
  Type: `Feat` `Fix` `Docs` `Style` `Refactor` `Test` `Chore`
    ```text
  예시)
        Refactor: 중복되는 DB 연결 로직을 싱글톤 패턴으로 분리
        
        - 서비스 레이어마다 흩어져 있던 Connection 코드를 DBContext로 통합
        - 코드 가독성 향상 및 리소스 낭비 방지
    ```

### 4. PR Convention

- 모든 변경 사항은 Issue 생성이 선행되어야 하며, 해당 이슈 번호를 관련 브랜치명과 PR 제목에 명시합니다.
    - Issue Description: 템플릿을 준수하며, 작업 목적과 체크리스트를 포함합니다.
    - PR Description: 템플릿을 준수하며, 변경 사항 요약, 관련 이슈 번호, 테스트 결과를 반드시 포함합니다.
- 소스 코드 변경이 완료되면 PR을 생성하고 셀프 코드 리뷰를 마친 후 `Code Rabbit`와 코드 리뷰를 진행합니다.
- PR 병합 시 `Squash Merge`를 사용하며 병합 시 커밋 메시지는 아래와 같이 작성합니다.
  ```text
  제목: Merge pull request(#pr-number): `main` ← `병합할 브랜치명`
  
  본문: [TYPE] PR 제목 (#issue-number) (#pr-number) ex) [FEAT] 핵심 비즈니스 도메인 모델링 (#1) (#2)
  ```
