# Development Conventions

이 문서는 `sytk` 프로젝트의 코드 품질과 일관성 있는 형상 관리를 위한 규칙을 정의합니다.

## 1. Branch Strategy
- **Workflow Strategy:** _Github Flow_
  > 충돌 확률이 적고 지속적 배포 환경에 최적화된 개인 프로젝트의 특성에 맞춤<br>
  > 속도감 있는 개발 리듬 및 빠른 피드백 위함
  > * `main` 브랜치 : 상용 환경에 배포되는 최신 상태 코드. 직접적인 커밋 금지
  > * 하위 브랜치(`feature` 브랜치) : 코드 수정을 위해 main에서 분기한 브랜치

- **Format:** `{Type}/{Module-Name}/{Issue-Number}-{Description}`
- **Module Name:** `root`, `booking`, `waiting`, `read`, `payment`
- **Types:**

  |   Type   | Description | Example                            |
  |:--------:|----------|------------------------------------|
  |   feat   | 새로운 기능 개발 | `feat/booking/12-login-api-implementation` |
  |   fix    | 버그 수정 | `fix/booking/12-login-error`               |
  |   docs   | 문서(README, API 문서 등) 수정 | `docs/root/12-update-readme`               |
  |  style   | 코드 포맷팅, UI 스타일 변경 (로직 변경 X) | `style/read/12-fix-margin`                 |
  | refactor | 코드 리팩토링(기능 변경 없이 구조만 개선) | `refactor/payment/12-clean-auth-logic`        |
  |   test   | 테스트 코드 작성 및 수정 | `test/waiting/12-user-service-test`           |
  |  chore   | 빌드 설정, 패키지 관리, 환경 설정 변경 | `chore/root/12-add-dotenv`                 |

## 2. PR & Issue Convention:
- 모든 변경 사항은 Issue 생성을 선행하고, 해당 이슈 번호를 브랜치 명과 PR에 명시합니다. 
  - **Issue Template:** 템플릿을 준수하며, 작업 목적과 체크리스트를 포함합니다. 
  - **PR Template:** 템플릿을 준수하며, 변경 사항 요약, 관련 이슈 번호, 테스트 결과를 반드시 포함합니다. 
- 변경이 완료되면 PR을 생성하고 셀프 코드리뷰를 마친 후 CodeRabbit의 리뷰를 확인합니다.
- 병합 시 기본적으로 `Squash Merge`로 진행하며 merge commit 메시지는 아래와 같이 작성합니다.
  ```text
    Merge pull request: `main` ← `병합할 브랜치명`
    
    - PR title (#issue-number) ex) [FEAT] 핵심 비즈니스 도메인 모델링 (#1)
  ```

## 3. Commit Message Convention
- **Format:** `Type: 작업 내용 한글로 요약` (본문은 필요시 추가)
- **Example:**
  ```text
    Refactor: 중복되는 DB 연결 로직을 싱글톤 패턴으로 분리               // 50자 이내, 대문자로 시작, 마침표 제거, 현재형/명령문
    
    - 서비스 레이어마다 흩어져 있던 Connection 코드를 DBContext로 통합   // 개발 이유 설명, 필수x
    - 코드 가독성 향상 및 리소스 낭비 방지
  ```
- **Types:**

    |   Type    | Description                 | Example |
    |:---------:|-----------------------------|-------|
    |   Feat    | 신규 기능 개발                    | `Feat: 네이버 로그인 API 연동` |
    |    Fix    | 버그 수정                       | `Fix: 로그인 시 토큰 만료 에러 해결` |
    |   Docs    | 문서(README, API 문서 등) 수정     | `Docs: API 명세서 최신화` |
    |   Style   | 코드 포맷팅, UI 스타일 변경 (로직 변경 X) | `Style: Prettier 적용 및 린트 에러 수정` |
    | Refactor  | 코드 개선(비즈니스 로직 변경 없이 구조만 개선) | `Refactor: 중복된 인증 로직 유틸화` |
    |   Test    | 테스트 코드 추가/수정                | `Test: 회원가입>  서비스 단위 테스트 추가` |
    |   Chore   | 빌드 업무, 패키지 관리, 환경 설정 변경 등   | `Chore: dotenv 라이브러리 추가` |

## 4. Coding Standards

### 4.1 Java & General Standards
- **Record 사용:** DTO 및 단순 데이터 캐리어는 record를 사용합니다.
- **Builder Pattern:** 객체 생성 시 생성자 파라미터가 3개 이상인 경우 @Builder를 사용합니다.
  - 단, 엔티티 내부에서 상태를 변경하는 비즈니스 메서드에는 `..Builder`보다 명확한 명칭의 메서드를 사용합니다. 
- **Exception Handling:** 비즈니스 예외를 활용하고, 전역 예외 Handler에서 처리합니다.

### 4.2 Rich Domain Model 기반 설계
- **Fat Service 경계 (Anemic Domain Model 방지):**
  - Service 계층은 도메인 객체를 조회하고, 도메인에 명령을 내리며, 저장하는 흐름 제어(Orchestration) 역할만 수행합니다. 
  - if 문을 통한 상태 검증, 복잡한 계산 로직 등 비즈니스 로직은 반드시 `domain` 엔티티 내부 메서드로 응집시켜 구현합니다.
  - 예: `seat.reserve()`, `reservation.confirm()`
- **캡슐화:** `좌석 선택 → 선점 → 결제 → 확정`으로 이어지는 상태 변화 시, 객체 스스로 자신의 상태를 검증하고 변경하게 합니다. 
- **Setter 사용 금지:**
  - 외부에서 엔티티의 필드를 직접 수정하는 것을 엄격히 금지합니다.
  - 도메인 객체는 항상 유효한 상태를 유지해야 하며, 생성자나 정적 팩토리 메서드에서 초기 상태 검증을 수행한다.

### 4.3 전파 방지 및 확장성 (Fault Tolerance)
- **연쇄 폭발 방지:** 특정 도메인의 변경이나 장애가 다른 도메인으로 전파되지 않도록 결합도를 낮춥니다.
- **도메인 분리:** 특정 도메인의 트래픽 급증 시 해당 도메인만 독립적인 서비스로 분리하거나 고성능 DB로 교체할 수 있도록 도메인 간 참조는 최소화(식별자 참조 권장)합니다.

## 5. API Design Guidelines
- **URI:** 복수형 리소스 명칭 사용 (`/api/v1/concerts`)
- **Case:** URI는 `kebab-case`, 파라미터 및 응답 바디는 `camelCase`를 사용합니다.
- **Stateless:** 모든 인증은 토큰(JWT 등) 기반으로 처리하며 서버는 세션 상태를 저장하지 않습니다.
- **HTTP Method:** 목적에 맞게 사용하며(RMM Level 2), 대표 코드가 아닌 적절한 상태 코드(200, 201, 400, 404, 500)를 응답한다.

## 6. Test Convention
- **Unit Test:** JUnit 5와 AssertJ, BDDMockito를 필수 사용합니다.
- **Naming:** 테스트 메서드 명은 `@DisplayName`을 통해 한글로 상세히 기술합니다.