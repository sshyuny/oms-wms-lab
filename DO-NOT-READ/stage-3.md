# 단계 3 — 출고 쪽이 느려지거나 실패하면 결제 완료까지 같이 실패함

> 단계 3 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.

## 심어 둔 원인
- 위치: `PaymentService.approve`가 하나의 `@Transactional` 안에서 순서대로 한다.
  1. `FakePgClient.approve` — 외부 PG 승인 (DB 롤백과 무관)
  2. `Payment` 저장
  3. 주문 `PAID` 변경
  4. `ShipmentService.createShipment` — 창고가 `SUSPENDED`면 `WAREHOUSE_SUSPENDED` 예외
- 4에서 예외가 나면 2·3은 롤백되지만 1은 그대로 남는다. 고객은 결제 실패 응답(409)을 받았는데 PG에는 승인 내역이 있다(돈이 빠짐). 주문은 `PENDING_PAYMENT`로 남는다.
- 재현: `PATCH /api/warehouses/1/status {"status":"SUSPENDED"}` 후 결제 → 409, `FakePgClient`에 `APPROVED` 거래가 남음, `payment` 테이블은 비어 있음.
- 반대 방향 결합도 있다: `ShipmentService.ship`이 `OrderRepository`로 주문 상태를 직접 바꾼다.
- 외부 PG 호출이 DB 트랜잭션 안에 있어 PG가 느리면 DB 커넥션을 오래 잡는다.

## 해법 방향 (참고)
- 결제 확정(결제 저장 + 주문 PAID)과 출고지시 생성을 트랜잭션으로 분리
- 결제 트랜잭션 안에서 아웃박스 테이블에 "결제 완료" 이벤트를 기록하고, 릴레이가 출고지시를 생성 (창고가 재개되면 재시도)
- 출고지시 생성은 멱등하게 (`shipment.order_id` unique를 이용하거나 이벤트 ID 기록)
- 주문·창고 모듈 경계 정리 (주문이 창고 내부 클래스를 직접 부르지 않게)

## 리뷰 체크포인트
- 이벤트 중복 전달 시 출고지시가 두 번 생기지 않는지
- 출고 완료 → 주문 상태 갱신도 같은 방식으로 분리했는지
- PG 승인 후 DB 저장 실패 시 보상(PG 취소) 처리
