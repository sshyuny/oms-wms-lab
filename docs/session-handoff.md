# 3단계 아웃박스 작업 인수인계

## 목표와 현재 위치

- 이슈: [#6 — 출고 처리가 느려지거나 실패하면 결제 완료까지 영향을 받음](https://github.com/sshyuny/oms-wms-lab/issues/6)
- 결제 시 출고 생성 직접 호출을 아웃박스 처리로 분리한다.
- 작업 목록과 결정 기록: [stage-3-outbox.md](stage-3-outbox.md)
- 1단계 설계, 2단계 기본 스키마, 3단계 결제 아웃박스 기록, 4단계 릴레이 프로덕션 코드까지 구현했다.
- 다음은 **5단계 실패·중복·동시 실행 대응**이다. 이후 6단계에서 재현·회귀 테스트를 진행한다.

## 역할과 읽기 범위

- 먼저 `AGENTS.md`, `README.md`, `docs/project-overview.md`, 이 문서와 작업 목록을 읽고 실제 Git 상태를 대조한다.
- 사용자는 아웃박스 첫 적용이라 AI와 개념·세부 설계를 함께 논의하고 필요하면 코드 예시를 요청한다. 최종 설계는 사용자가 결정하며, 프로젝트 코드 작성·수정은 구체적인 지시를 받은 뒤 진행한다.
- 다음 세션에서는 맥락을 파악한 뒤 사용자의 5단계 논의를 기다린다. 선제 구현하지 않는다.
- 기존 허용 범위는 `DO-NOT-READ/stage-3.md`의 해답·실제 해결 기록까지다. 이번 세션에서 읽었으며, 다른 단계 파일과 `base-plan.md`는 읽지 않는다. 새 세션에서 맥락 복원만을 위해 재열람할 필요는 없다.

## Git 상태

- 브랜치: `bugfix/ship-1/outbox`
- 문서 갱신 직전 HEAD: `90f07e7` — 출고 이벤트 릴레이 구현 상태를 기록한다
- 문서 갱신 직전 작업 트리는 깨끗했고, 로컬 추적 정보 기준 원격보다 9커밋 앞서 있었다. 이 세션에서 push하지 않았다.
- 인수인계 문서는 별도 문서 커밋으로 남긴다. 새 세션에서는 실제 HEAD와 작업 트리를 확인한다.
- 이전 시도 [PR #7](https://github.com/sshyuny/oms-wms-lab/pull/7)은 머지 없이 닫혔다. 이전 브랜치 `bugfix/ship-1/shipment-payment-failure`는 보존하기로 했다.
- 사용자 작성 코드만 커밋할 때는 공동 작성자 표시를 붙이지 않는다. AI가 작성·수정한 코드나 문서가 포함된 커밋에만 표시한다.

## 합의와 구현

- 공용 `outbox_event` 테이블: `id`, `event_type`, JSON `payload`, `status`, `created_at`, `processed_at`, `(status, id)` 인덱스.
- `id`가 메시지 고유 ID다. 이벤트는 `SHIPMENT_CREATION_REQUESTED`, payload는 `{"orderId": 123}`이다.
- `OutboxEvent`는 생성자에서 `PENDING`과 생성 시각을 설정하며, Hibernate JSON 매핑을 사용한다.
- `V2__create_outbox_event.sql`로 테이블을 생성한다.
- `PaymentService.approve()`가 `ShipmentEventRepository.publishShipmentCreation()`을 호출한다. 결제 저장·주문 상태 변경·아웃박스 기록은 같은 트랜잭션에 참여한다.
- `ShipmentScheduler`는 `@Scheduled(fixedRate = 10000)`으로 미처리 출고 이벤트를 조회한다. `JpaConfig`에서 `@EnableScheduling`을 활성화했다.
- 전달은 같은 애플리케이션의 `ShipmentService.createShipment()` 메서드 호출이다. HTTP·메시지 브로커는 사용하지 않는다.
- 성공 후 `PROCESSED`와 현재 처리 완료 시각을 저장한다. 출고 생성 예외는 로그로 남기며 DB의 이벤트는 `PENDING`으로 남는다.
- 실패 로그에는 이벤트 ID, 주문 ID, 예외 스택 트레이스가 포함된다.

## 5단계에 남긴 사항

아래는 리뷰에서 남긴 검토 항목이며, 구체적인 대응 설계는 아직 정하지 않았다.

- 출고 생성 성공 후 이벤트 완료 저장 전에 중단되거나 저장이 실패하는 경우
- 동일 이벤트 재처리와 여러 릴레이의 동시 처리
- 특정 이벤트의 반복 실패와 재시도 정책, 필요한 추가 컬럼
- payload 역직렬화와 주문 ID 변환이 `try` 밖에 있어, 예외가 나면 해당 실행의 나머지 이벤트 처리가 중단되는 문제

## 검증 결과와 한계

- 2단계에서 `./gradlew test` 실행: 실제 MySQL에서 Flyway V1 → V2 적용 성공, 기존 테스트 16개 통과, 실패·건너뜀 없음.
- 3·4단계 코드 리뷰 중 `./gradlew compileJava`가 통과했다.
- 4단계 최종 커밋의 주문 ID 변수 추출·로그 변경은 코드로만 확인했고, 그 이후 컴파일은 다시 실행하지 않았다.
- 3·4단계 이후 전체 테스트는 실행하지 않았다.
- 실제 JSON 저장·조회, 결제와 아웃박스의 동시 롤백, 스케줄러 자동 실행, 창고 중지·재개, 지연·중복·동시 실행은 아직 검증하지 않았다.
- 문서 커밋 시 공백 검사는 통과했다.

## 주요 파일

- `src/main/java/com/sshyuny/omswms/common/OutboxEvent.java`
- `src/main/java/com/sshyuny/omswms/common/OutboxEventJpaRepository.java`
- `src/main/java/com/sshyuny/omswms/common/JpaConfig.java`
- `src/main/java/com/sshyuny/omswms/payment/PaymentService.java`
- `src/main/java/com/sshyuny/omswms/shipment/ShipmentEventRepository.java`
- `src/main/java/com/sshyuny/omswms/shipment/ShipmentScheduler.java`
- `src/main/java/com/sshyuny/omswms/shipment/ShipmentService.java`
- `src/main/resources/db/migration/V2__create_outbox_event.sql`

테스트 초기화 참고: 마지막 확인 시 `src/test/java/com/sshyuny/omswms/IntegrationTest.java`의 초기화 대상에 `outbox_event`가 없었다. 테스트 작업 시 현재 상태를 확인한다.
