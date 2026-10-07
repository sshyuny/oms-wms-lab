# 단계 1 — 주문이 몰리면 재고가 음수가 됨

> 단계 1 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.

## 심어 둔 원인
- 위치: `StockService.decrease` → `StockRepository.decreaseQuantity`
- 재고를 조회해서 `quantity < 요청수량`인지 검사한 뒤, 차감은 별도 쿼리 `update stock set quantity = quantity - :q`로 한다.
- 검사와 차감 사이에 락이 없다. 동시 요청이 같은 수량을 읽고 둘 다 검사를 통과하면, 차감 쿼리 자체는 원자적이라 값이 그대로 빠져 음수가 된다.
- "차감을 원자적 update로 했으니 안전하다"는 착각을 노린 함정이다. 엔티티 setter로 차감했다면 음수 대신 갱신 손실(lost update)로 나타났을 것이다.

## 재현 방법 예시
- 재고 1개, 스레드 2~10개가 동시에 주문 생성 (`ExecutorService` + `CountDownLatch`).
- 테스트 메서드에 `@Transactional`을 붙이면 동시성이 재현되지 않는다. 각 요청이 각자 커밋돼야 한다.
- 기대: 성공 주문 수 ≤ 재고, 최종 재고 ≥ 0.

## 해법 방향 (참고, 사용자 해법이 달라도 됨)
- 조건부 update: `... set quantity = quantity - :q where product_id = :id and quantity >= :q` + 영향받은 행 수가 0이면 재고 부족
- 비관적 락: `select ... for update`로 조회 후 검사·차감
- 낙관적 락: `@Version` + 재시도

## 리뷰 체크포인트
- 여러 상품을 담은 주문에서 비관적 락을 쓰면 상품 잠금 순서가 요청마다 달라 데드락 가능 → productId 정렬 등
- 한 주문에 같은 상품이 두 줄 들어오는 경우 (현재 검사는 영속성 컨텍스트의 오래된 값을 볼 수 있음)
- 재현 테스트가 실제로 동시에 실행되는지, 실패 전에 한 번은 빨간불을 봤는지
