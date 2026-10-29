# oms-wms-lab

주문(OMS)과 창고(WMS)가 만나는 지점의 문제를 단계별로 풀어 보는 개인 프로젝트.

## 이 리포를 읽는 방법

- **base(첫 커밋)는 의도적으로 단순하게 만든 출발점이다.** "이미 돌아가는 회사 프로젝트에 합류한 상황"을 재현하려고 AI(Claude)에게 만들게 했다. base 코드는 내 작업이 아니다.
- base에는 버그·장애를 의도적으로 남겨 두었고, 신규 요구사항도 단계별로 준비해 두었다. 증상이나 요구만 적은 GitHub Issue를 단계마다 하나씩 받아, 이를 해결하는 커밋을 만들고 PR로 올리는 방식으로 작업한다. 증상 재현, 원인 진단, 해법 결정은 내가 했다. 코드는 직접 작성하거나, 구체적으로 지시해 AI(Claude)에게 작성하게 했고, AI가 작성한 커밋에는 `Co-Authored-By`를 남겼다.
- `DO-NOT-READ/`에는 base를 만들 때 미리 적어 둔 단계별 이슈 티켓의 정답지(원인과 위치)가 들어 있다. 나는 단계를 끝낸 뒤에만 해당 파일을 열어 내 진단과 비교했다.
- 단계마다 진행 흐름은 아래와 같다.
  1. 증상만 적힌 이슈를 받는다.
  2. `bugfix/<티켓키>/<설명>` 브랜치에서 증상을 실패하는 테스트로 재현하고, 원인을 찾아 고친다.
  3. PR에 원인, 선택한 해법, 버린 대안을 적고 squash merge한다.
  4. 정답지(`DO-NOT-READ/stage-N.md`)를 열어 내 진단과 비교하고, 실제로 쓴 방법과 남은 리뷰 체크포인트를 그 파일 끝 "실제 해결 기록"에 남긴다.
- 아래 "단계별 기록" 표에는 단계마다 한 줄만 적는다. 자세한 내용은 각 PR과 `stage-N.md`의 "실제 해결 기록"에 있다.

## 실행

Docker(OrbStack 등)가 필요하다. 테스트와 로컬 실행 모두 Testcontainers로 MySQL 8.4를 띄운다.

```bash
./gradlew test          # 전체 테스트
./gradlew bootTestRun   # MySQL 컨테이너와 함께 애플리케이션 실행 (http://localhost:8080)
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/products` | 상품 등록 (재고 0개로 함께 생성) |
| GET | `/api/products/{id}` | 상품 조회 |
| PUT | `/api/stocks/{productId}` | 재고 수량 변경 |
| GET | `/api/stocks/{productId}` | 재고 조회 |
| GET | `/api/warehouses/{id}` | 창고 조회 |
| PATCH | `/api/warehouses/{id}/status` | 창고 출고 상태 변경 (`ACTIVE` / `SUSPENDED`) |
| POST | `/api/coupons` | 쿠폰 등록 |
| POST | `/api/orders` | 주문 생성 |
| GET | `/api/orders/{id}` | 주문 조회 |
| POST | `/api/orders/{id}/payment` | 결제 승인 |
| POST | `/api/orders/{id}/cancel` | 주문 취소 |
| GET | `/api/shipments/{id}` | 출고 조회 |
| GET | `/api/shipments?orderId=` | 주문의 출고 조회 |
| POST | `/api/shipments/{id}/picking` | 피킹 시작 |
| POST | `/api/shipments/{id}/packing` | 패킹 완료 |
| POST | `/api/shipments/{id}/shipping` | 출고 완료 |

## base의 업무 규칙

- 창고는 1곳(`WH-01`)이다. 창고는 출고를 일시 중지(`SUSPENDED`)할 수 있다 (재고 실사 등).
- 배송비는 3,000원이고, 상품 금액 합계가 50,000원 이상이면 무료다.
- 쿠폰은 정액 할인 코드형이다. 최소 주문 금액이 있고, 사용 횟수 제한은 없다.
- 고객은 ID 값으로만 존재한다 (회원·인증 없음).
- 결제는 가짜 PG(`FakePgClient`)로 승인·취소한다.
- 주문 상태: `PENDING_PAYMENT` → `PAID` → `SHIPPED`, 또는 `CANCELLED`
- 출고 상태: `READY` → `PICKING` → `PACKED` → `SHIPPED`, 또는 `CANCELLED`
- 주문 취소는 결제 전이거나 출고가 `READY`일 때만, 전체 취소만 가능하다.

## 용어

(단계를 진행하며 작성)

## 단계별 기록

| 단계 | 티켓 | 내용 |
|---|---|---|
| 0 | — | 온보딩 |
| 1 | [#2](https://github.com/sshyuny/oms-wms-lab/issues/2) | 동시 주문 시 재고가 음수로 떨어지는 문제 → 조건부 UPDATE로 차감 |
| 2 | [#4](https://github.com/sshyuny/oms-wms-lab/issues/4) | 미결제 주문에 묶인 재고로 신규 주문이 막히는 문제 → 신규 주문 시 10분 초과 미결제 주문 취소와 재고 복구 |

(단계를 진행하며 작성)
