# oms-wms-lab base 프로젝트 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ⚠️ 이 파일은 스포일러다. base에 심어 둔 한계가 전부 들어 있다. 사용자는 이 파일을 읽지 않는다 (설계 결정 B9).

**Goal:** 주문 → 결제 → 출고지시 → 출고완료 → 전체취소가 창고 1개로 끝까지 돌아가는, "흔한 실무 코드" 수준의 Spring Boot base 프로젝트를 만든다. 단계 1~6의 한계를 의도적으로 심고, 원인을 `DO-NOT-READ/stage-N.md`에 함께 커밋한다.

**Architecture:** 도메인별 패키지(`product`, `stock`, `warehouse`, `coupon`, `order`, `payment`, `shipment`) 안에 Controller → Service → Repository → Entity를 평평하게 둔다. 로직은 서비스에 있고 엔티티는 Lombok `@Getter/@Setter`로 데이터만 든다. 도메인 간 호출은 서비스·리포지토리를 직접 주입해서 같은 트랜잭션 안에서 처리한다.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Data JPA (Hibernate), Flyway, MySQL 8.4 (Testcontainers 2.x), Lombok, Gradle 9.7.1 wrapper, JUnit 5 + MockMvc

**Spec:** `~/Documents/00.sohyun/md-files/프로젝트/사이드프로젝트 OMS-WMS base 프로젝트 설계.md`

## Global Constraints

- Java toolchain **25** (스펙의 "Java 21"은 21 이상 의미. 로컬에 21이 없고 Corretto 25 LTS가 기본 JDK)
- Spring Boot **4.1.1**, Gradle wrapper **9.7.1** (Spring Initializr 생성본 그대로)
- 테스트 DB는 **MySQL 8.4 Testcontainers**. H2 금지
- Gradle **단일 모듈**
- **회사 코드·스킬·용어·컨벤션 참고 금지 (B12)**. `java-spring-workflow-from-j` 규칙도 적용하지 않는다
- base 코드 스타일 (B5): 명사형 서비스(`OrderService`), 엔티티 `@Getter @Setter @NoArgsConstructor(access = PROTECTED)`, 서비스 안에서 `LocalDateTime.now()` 직접 호출, `@Transactional(readOnly = true)` 클래스 기본 + 쓰기 메서드 `@Transactional`, 요청·응답 DTO는 `record`
- 테스트는 **정상 경로만** (B5). 오류 경로 테스트는 커밋하지 않는다
- **심어 둔 한계를 구현 중에 고치지 않는다.** 아래 "심어 둔 한계" 표의 코드는 계획 그대로 쓴다
- 코드 주석·README·커밋 메시지에 한계를 암시하는 표현을 쓰지 않는다
- DB 외래키는 만들지 않는다
- 커밋 메시지는 한국어, 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## 심어 둔 한계 (요약 — 상세는 Task 8의 stage 파일)

| 단계 | 위치 | 한계 |
|---|---|---|
| 1 | `StockService.decrease` | 재고 조회 후 검사하고, 차감은 별도 `update ... quantity - :q` 쿼리. 동시 요청이 검사를 같이 통과해 음수 |
| 2 | `OrderService.createOrder` | 주문 생성 시점에 재고 차감. 결제 대기 주문은 만료되지 않음. 재고는 수량 컬럼 하나 |
| 3 | `PaymentService.approve` | PG 승인 → 결제 저장 → 주문 PAID → 출고지시 생성이 한 트랜잭션. 창고 출고 중지면 롤백되지만 PG 승인은 남음 |
| 4 | `ShipmentService`, `stock`, `shipment` 스키마 | 창고는 설정값 하나(`app.default-warehouse-id`). 재고는 상품당 1행, 출고는 주문당 1건(`unique order_id`). 금액은 주문 단위로만 있음 |
| 5 | `OrderService.cancelOrder` | 전체 취소만, 출고 READY일 때만. 출고 상태 확인 후 변경까지 락·버전 없음 → 피킹 시작과 경합. PG는 전액 취소만 |
| 6 | 전체 | 출고완료 이후 흐름 없음. 주문 SHIPPED가 종착 상태 |

## Review Focus

B5(정상 경로만 테스트)에 따라 아래 오류 경로는 **테스트를 커밋하지 않는다**. 대신 Task 10에서 임시 테스트로 동작만 확인하고 지운다. 사용자가 단계 작업 중에 엉뚱한 버그를 만나지 않게 하기 위함이다.

1. 없는 상품·주문·출고 ID 조회 → 404와 `{"code": "...", "message": "..."}` 본문
2. 필수 필드 누락 요청 → 400 `INVALID_INPUT`
3. 쿠폰 최소 주문 금액 미달 → 400 `COUPON_MIN_ORDER_AMOUNT_NOT_MET`, 재고는 차감되지 않음(롤백)
4. 출고 상태 순서 위반(READY에서 바로 packing) → 409 `INVALID_SHIPMENT_STATUS`
5. 피킹 시작된 주문 취소 → 409 `INVALID_SHIPMENT_STATUS`

## 파일 구조

```
oms-wms-lab/
├── .claude/settings.json          # DO-NOT-READ 읽기 ask 권한 (B11)
├── .ignore                        # rg 검색 제외 (B7)
├── AGENTS.md                      # AI 에이전트 규칙 (B10)
├── CLAUDE.md                      # @AGENTS.md 임포트
├── README.md
├── DO-NOT-READ/
│   ├── base-plan.md               # 이 파일
│   └── stage-1.md ~ stage-6.md
├── build.gradle, settings.gradle, gradlew, gradle/
└── src/
    ├── main/java/com/sshyuny/omswms/
    │   ├── OmsWmsLabApplication.java
    │   ├── common/        BaseTimeEntity, JpaConfig, ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
    │   ├── product/       Product, ProductRepository, ProductService, ProductController, ProductCreateRequest, ProductResponse
    │   ├── stock/         Stock, StockRepository, StockService, StockController, StockUpdateRequest, StockResponse
    │   ├── warehouse/     Warehouse, WarehouseStatus, WarehouseRepository, WarehouseService, WarehouseController, WarehouseStatusChangeRequest, WarehouseResponse
    │   ├── coupon/        Coupon, CouponRepository, CouponService, CouponController, CouponCreateRequest, CouponResponse
    │   ├── order/         Order, OrderLine, OrderStatus, OrderRepository, OrderService, OrderController, OrderCreateRequest, OrderLineRequest, OrderResponse, OrderLineResponse
    │   ├── payment/       Payment, PaymentStatus, PaymentRepository, PaymentService, PaymentController, PaymentResponse, FakePgClient, PgTransaction, PgTransactionStatus
    │   └── shipment/      Shipment, ShipmentStatus, ShipmentRepository, ShipmentService, ShipmentController, ShipmentResponse
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__init.sql
    └── test/java/com/sshyuny/omswms/
        ├── OmsWmsLabApplicationTests.java, TestOmsWmsLabApplication.java, TestcontainersConfiguration.java
        ├── IntegrationTest.java, SchemaTest.java
        ├── product/ProductApiTest.java
        ├── stock/StockApiTest.java
        ├── warehouse/WarehouseApiTest.java
        ├── coupon/CouponApiTest.java
        ├── order/OrderApiTest.java, order/OrderCancelApiTest.java
        ├── payment/PaymentApiTest.java
        └── shipment/ShipmentApiTest.java
```

---

### Task 0: 실행 전 확인 (사용자 결정 필요)

- [ ] **Step 1: Docker 확인.** `docker info --format '{{.ServerVersion}}'`가 실패하면 `open -a OrbStack` 후 재확인. 안 되면 사용자에게 OrbStack 실행 요청
- [ ] **Step 2: 브랜치 방식 결정을 사용자에게 묻는다.**
  - (a) PR 방식(추천): main에 초기화 커밋 하나 → 사용자가 main push → `base` 브랜치에서 개발 → PR #1을 **squash merge** → base가 main의 커밋 하나로 분리됨
  - (b) main 직접: 전역 규칙의 개인 리포 예외로 main 커밋은 가능, push는 사용자가 직접. base가 여러 커밋으로 남음
- [ ] **Step 3: GitHub 리포 생성 동의를 받는다** (외부에 리소스를 만드는 작업). `gh auth status`가 `sshyuny`인지 확인

### Task 1: 프로젝트 골격, 스키마, 테스트 기반

**Files:**
- Create: Initializr 생성 파일 전체 (`build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`, `.gitattributes`, `src/main/java/com/sshyuny/omswms/OmsWmsLabApplication.java`, `src/test/java/com/sshyuny/omswms/{OmsWmsLabApplicationTests,TestOmsWmsLabApplication,TestcontainersConfiguration}.java`)
- Delete: `HELP.md`, `src/main/resources/application.properties`
- Create: `src/main/resources/application.yml`, `src/main/resources/db/migration/V1__init.sql`
- Create: `src/main/java/com/sshyuny/omswms/common/{BaseTimeEntity,JpaConfig,ErrorCode,BusinessException,ErrorResponse,GlobalExceptionHandler}.java`
- Create: `src/test/java/com/sshyuny/omswms/{IntegrationTest,SchemaTest}.java`

**Interfaces:**
- Produces: `IntegrationTest` (protected `mockMvc`, `jdbcTemplate`, `postJson(String url, String body, ResultMatcher expectedStatus): String`), `BusinessException(ErrorCode)`, `ErrorCode` 전체 상수, `BaseTimeEntity`

- [ ] **Step 1: 리포 초기화**

```bash
cd ~/my-projects/oms-wms-lab
git init -b main
gh repo create sshyuny/oms-wms-lab --private --source=. --remote=origin
git remote get-url origin   # https://github.com/sshyuny/oms-wms-lab.git 확인
```

Task 0에서 (a)를 골랐다면:

```bash
printf '# oms-wms-lab\n' > README.md
printf '.gradle/\nbuild/\n.idea/\n*.iml\n.DS_Store\n' > .gitignore
git add README.md .gitignore
git commit -m "chore: 리포지토리 초기화" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

사용자에게 `git push -u origin main` 실행을 요청하고, 완료 후 `git switch -c base`.

- [ ] **Step 2: Initializr 골격 내려받기**

```bash
cd ~/my-projects/oms-wms-lab
curl -s -o /tmp/oms-starter.zip "https://start.spring.io/starter.zip?type=gradle-project&language=java&bootVersion=4.1.1&javaVersion=25&groupId=com.sshyuny&artifactId=oms-wms-lab&name=oms-wms-lab&packageName=com.sshyuny.omswms&dependencies=web,data-jpa,mysql,flyway,testcontainers,validation,lombok"
unzip -o /tmp/oms-starter.zip -d .
rm -f HELP.md src/main/resources/application.properties /tmp/oms-starter.zip
```

(a) 방식이면 Step 1에서 만든 `.gitignore`는 Initializr 것으로 덮인다. 덮인 파일 끝에 `.DS_Store`가 없으면 추가한다.

- [ ] **Step 3: `build.gradle` 확인**

Initializr 결과가 아래와 같아야 한다. 다르면 아래 내용으로 맞춘다.

```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.1.1'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.sshyuny'
version = '0.0.1-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-flyway'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.flywaydb:flyway-mysql'
	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'com.mysql:mysql-connector-j'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-flyway-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-validation-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
	testImplementation 'org.testcontainers:testcontainers-mysql'
	testCompileOnly 'org.projectlombok:lombok'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
	useJUnitPlatform()
}
```

- [ ] **Step 4: MySQL 이미지 고정**

`src/test/java/com/sshyuny/omswms/TestcontainersConfiguration.java`:

```java
package com.sshyuny.omswms;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

}
```

- [ ] **Step 5: `application.yml` 작성**

```yaml
spring:
  application:
    name: oms-wms-lab
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true

app:
  default-warehouse-id: 1
```

- [ ] **Step 6: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/IntegrationTest.java`:

```java
package com.sshyuny.omswms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    private static final List<String> TABLES = List.of(
            "product", "stock", "coupon", "orders", "order_line", "payment", "shipment");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUpDatabase() {
        TABLES.forEach(table -> jdbcTemplate.execute("truncate table " + table));
        jdbcTemplate.update("update warehouse set status = 'ACTIVE'");
    }

    protected String postJson(String url, String body, ResultMatcher expectedStatus) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(expectedStatus)
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
```

`src/test/java/com/sshyuny/omswms/SchemaTest.java`:

```java
package com.sshyuny.omswms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SchemaTest extends IntegrationTest {

    @Test
    void 기본_창고가_등록되어_있다() {
        String code = jdbcTemplate.queryForObject("select code from warehouse where id = 1", String.class);

        assertThat(code).isEqualTo("WH-01");
    }
}
```

- [ ] **Step 7: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.SchemaTest'`
Expected: FAIL (`Table 'test.product' doesn't exist` 류의 오류)

- [ ] **Step 8: 스키마 작성**

`src/main/resources/db/migration/V1__init.sql`:

```sql
create table product (
    id          bigint       not null auto_increment,
    sku_code    varchar(50)  not null,
    name        varchar(200) not null,
    price       bigint       not null,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    primary key (id),
    unique key uk_product_sku_code (sku_code)
);

create table warehouse (
    id          bigint       not null auto_increment,
    code        varchar(50)  not null,
    name        varchar(100) not null,
    status      varchar(20)  not null,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    primary key (id),
    unique key uk_warehouse_code (code)
);

insert into warehouse (code, name, status, created_at, updated_at)
values ('WH-01', '기본 물류센터', 'ACTIVE', now(6), now(6));

create table stock (
    id          bigint      not null auto_increment,
    product_id  bigint      not null,
    quantity    int         not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null,
    primary key (id),
    unique key uk_stock_product_id (product_id)
);

create table coupon (
    id                bigint      not null auto_increment,
    code              varchar(50) not null,
    discount_amount   bigint      not null,
    min_order_amount  bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    primary key (id),
    unique key uk_coupon_code (code)
);

create table orders (
    id                      bigint      not null auto_increment,
    customer_id             bigint      not null,
    status                  varchar(20) not null,
    total_product_amount    bigint      not null,
    shipping_fee            bigint      not null,
    coupon_code             varchar(50),
    coupon_discount_amount  bigint      not null,
    payment_amount          bigint      not null,
    ordered_at              datetime(6) not null,
    paid_at                 datetime(6),
    cancelled_at            datetime(6),
    created_at              datetime(6) not null,
    updated_at              datetime(6) not null,
    primary key (id),
    key idx_orders_customer_id (customer_id)
);

create table order_line (
    id            bigint       not null auto_increment,
    order_id      bigint       not null,
    product_id    bigint       not null,
    product_name  varchar(200) not null,
    unit_price    bigint       not null,
    quantity      int          not null,
    line_amount   bigint       not null,
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    primary key (id),
    key idx_order_line_order_id (order_id)
);

create table payment (
    id                 bigint      not null auto_increment,
    order_id           bigint      not null,
    pg_transaction_id  varchar(64) not null,
    amount             bigint      not null,
    status             varchar(20) not null,
    approved_at        datetime(6) not null,
    cancelled_at       datetime(6),
    created_at         datetime(6) not null,
    updated_at         datetime(6) not null,
    primary key (id),
    unique key uk_payment_order_id (order_id)
);

create table shipment (
    id            bigint      not null auto_increment,
    order_id      bigint      not null,
    warehouse_id  bigint      not null,
    status        varchar(20) not null,
    shipped_at    datetime(6),
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    primary key (id),
    unique key uk_shipment_order_id (order_id)
);
```

- [ ] **Step 9: 공통 클래스 작성**

`src/main/java/com/sshyuny/omswms/common/BaseTimeEntity.java`:

```java
package com.sshyuny.omswms.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

`src/main/java/com/sshyuny/omswms/common/JpaConfig.java`:

```java
package com.sshyuny.omswms.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

`src/main/java/com/sshyuny/omswms/common/ErrorCode.java`:

```java
package com.sshyuny.omswms.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "창고를 찾을 수 없습니다."),
    WAREHOUSE_SUSPENDED(HttpStatus.CONFLICT, "출고가 중지된 창고입니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "쿠폰 최소 주문 금액을 충족하지 않습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "현재 주문 상태에서 처리할 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "출고 정보를 찾을 수 없습니다."),
    INVALID_SHIPMENT_STATUS(HttpStatus.CONFLICT, "현재 출고 상태에서 처리할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
```

`src/main/java/com/sshyuny/omswms/common/BusinessException.java`:

```java
package com.sshyuny.omswms.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

`src/main/java/com/sshyuny/omswms/common/ErrorResponse.java`:

```java
package com.sshyuny.omswms.common;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
```

`src/main/java/com/sshyuny/omswms/common/GlobalExceptionHandler.java`:

```java
package com.sshyuny.omswms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("business exception: {}", errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("validation exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }
}
```

- [ ] **Step 10: 전체 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (`OmsWmsLabApplicationTests.contextLoads`, `SchemaTest`)

- [ ] **Step 11: 커밋**

```bash
git add -A -- . ':!DO-NOT-READ'
git commit -m "chore: 프로젝트 골격과 초기 스키마 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 2: 상품·재고

**Files:**
- Create: `src/main/java/com/sshyuny/omswms/product/{Product,ProductRepository,ProductService,ProductController,ProductCreateRequest,ProductResponse}.java`
- Create: `src/main/java/com/sshyuny/omswms/stock/{Stock,StockRepository,StockService,StockController,StockUpdateRequest,StockResponse}.java`
- Modify: `src/test/java/com/sshyuny/omswms/IntegrationTest.java` (헬퍼 추가)
- Test: `src/test/java/com/sshyuny/omswms/product/ProductApiTest.java`, `src/test/java/com/sshyuny/omswms/stock/StockApiTest.java`

**Interfaces:**
- Consumes: `IntegrationTest.postJson`, `BusinessException`, `ErrorCode.PRODUCT_NOT_FOUND`, `ErrorCode.STOCK_NOT_FOUND`, `ErrorCode.OUT_OF_STOCK`
- Produces: `ProductRepository extends JpaRepository<Product, Long>`, `Product#getId/getName/getPrice`, `StockService#decrease(Long productId, int quantity)`, `StockService#increase(Long productId, int quantity)`, 테스트 헬퍼 `createProduct(String skuCode, long price): Long`, `setStock(Long productId, int quantity)`

- [ ] **Step 1: 테스트 헬퍼 추가**

`IntegrationTest`에 import 추가: `com.jayway.jsonpath.JsonPath`, `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put`, `static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status`. 클래스 끝에 메서드 추가:

```java
    protected Long createProduct(String skuCode, long price) throws Exception {
        String response = postJson("/api/products", """
                {"skuCode": "%s", "name": "테스트 상품 %s", "price": %d}
                """.formatted(skuCode, skuCode, price), status().isCreated());
        return JsonPath.parse(response).read("$.id", Long.class);
    }

    protected void setStock(Long productId, int quantity) throws Exception {
        mockMvc.perform(put("/api/stocks/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": %d}
                                """.formatted(quantity)))
                .andExpect(status().isOk());
    }
```

- [ ] **Step 2: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/product/ProductApiTest.java`:

```java
package com.sshyuny.omswms.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class ProductApiTest extends IntegrationTest {

    @Test
    void 상품을_등록하고_조회한다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.skuCode").value("SKU-001"))
                .andExpect(jsonPath("$.price").value(10_000));
    }

    @Test
    void 상품을_등록하면_재고_0개가_함께_생성된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.soldOut").value(true));
    }
}
```

`src/test/java/com/sshyuny/omswms/stock/StockApiTest.java`:

```java
package com.sshyuny.omswms.stock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class StockApiTest extends IntegrationTest {

    @Test
    void 재고_수량을_변경하고_조회한다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);

        setStock(productId, 10);

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.soldOut").value(false));
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.product.*' --tests 'com.sshyuny.omswms.stock.*'`
Expected: FAIL (`/api/products` 404, 상태 코드 불일치)

- [ ] **Step 4: 상품 구현**

`product/Product.java`:

```java
package com.sshyuny.omswms.product;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Long price;

    public Product(String skuCode, String name, Long price) {
        this.skuCode = skuCode;
        this.name = name;
        this.price = price;
    }
}
```

`product/ProductRepository.java`:

```java
package com.sshyuny.omswms.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

`product/ProductCreateRequest.java`:

```java
package com.sshyuny.omswms.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductCreateRequest(
        @NotBlank String skuCode,
        @NotBlank String name,
        @NotNull @Positive Long price
) {
}
```

`product/ProductResponse.java`:

```java
package com.sshyuny.omswms.product;

public record ProductResponse(Long id, String skuCode, String name, Long price) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSkuCode(), product.getName(), product.getPrice());
    }
}
```

`product/ProductService.java`:

```java
package com.sshyuny.omswms.product;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.stock.Stock;
import com.sshyuny.omswms.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = productRepository.save(
                new Product(request.skuCode(), request.name(), request.price()));
        stockRepository.save(new Stock(product.getId(), 0));
        return ProductResponse.from(product);
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }
}
```

`product/ProductController.java`:

```java
package com.sshyuny.omswms.product;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }
}
```

- [ ] **Step 5: 재고 구현** (⚠️ `decrease`는 단계 1의 심어 둔 한계. 그대로 쓴다)

`stock/Stock.java`:

```java
package com.sshyuny.omswms.stock;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    public Stock(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
```

`stock/StockRepository.java`:

```java
package com.sshyuny.omswms.stock;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);

    @Modifying(flushAutomatically = true)
    @Query("update Stock s set s.quantity = s.quantity - :quantity where s.productId = :productId")
    int decreaseQuantity(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("update Stock s set s.quantity = s.quantity + :quantity where s.productId = :productId")
    int increaseQuantity(@Param("productId") Long productId, @Param("quantity") int quantity);
}
```

`stock/StockUpdateRequest.java`:

```java
package com.sshyuny.omswms.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockUpdateRequest(@NotNull @PositiveOrZero Integer quantity) {
}
```

`stock/StockResponse.java`:

```java
package com.sshyuny.omswms.stock;

public record StockResponse(Long productId, Integer quantity, boolean soldOut) {

    public static StockResponse from(Stock stock) {
        return new StockResponse(stock.getProductId(), stock.getQuantity(), stock.getQuantity() <= 0);
    }
}
```

`stock/StockService.java`:

```java
package com.sshyuny.omswms.stock;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;

    @Transactional
    public StockResponse updateQuantity(Long productId, StockUpdateRequest request) {
        Stock stock = getStockEntity(productId);
        stock.setQuantity(request.quantity());
        return StockResponse.from(stock);
    }

    public StockResponse getStock(Long productId) {
        return StockResponse.from(getStockEntity(productId));
    }

    @Transactional
    public void decrease(Long productId, int quantity) {
        Stock stock = getStockEntity(productId);
        if (stock.getQuantity() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        stockRepository.decreaseQuantity(productId, quantity);
    }

    @Transactional
    public void increase(Long productId, int quantity) {
        stockRepository.increaseQuantity(productId, quantity);
    }

    private Stock getStockEntity(Long productId) {
        return stockRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
    }
}
```

`stock/StockController.java`:

```java
package com.sshyuny.omswms.stock;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PutMapping("/{productId}")
    public StockResponse updateQuantity(@PathVariable Long productId,
                                        @Valid @RequestBody StockUpdateRequest request) {
        return stockService.updateQuantity(productId, request);
    }

    @GetMapping("/{productId}")
    public StockResponse getStock(@PathVariable Long productId) {
        return stockService.getStock(productId);
    }
}
```

- [ ] **Step 6: 통과 확인**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add src
git commit -m "feat: 상품 등록과 재고 관리 API 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 3: 창고·쿠폰

**Files:**
- Create: `src/main/java/com/sshyuny/omswms/warehouse/{Warehouse,WarehouseStatus,WarehouseRepository,WarehouseService,WarehouseController,WarehouseStatusChangeRequest,WarehouseResponse}.java`
- Create: `src/main/java/com/sshyuny/omswms/coupon/{Coupon,CouponRepository,CouponService,CouponController,CouponCreateRequest,CouponResponse}.java`
- Test: `src/test/java/com/sshyuny/omswms/warehouse/WarehouseApiTest.java`, `src/test/java/com/sshyuny/omswms/coupon/CouponApiTest.java`

**Interfaces:**
- Produces: `WarehouseRepository extends JpaRepository<Warehouse, Long>`, `WarehouseStatus.{ACTIVE, SUSPENDED}`, `Warehouse#getStatus/getId`, `CouponRepository#findByCode(String): Optional<Coupon>`, `Coupon#getDiscountAmount/getMinOrderAmount`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/warehouse/WarehouseApiTest.java`:

```java
package com.sshyuny.omswms.warehouse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class WarehouseApiTest extends IntegrationTest {

    @Test
    void 창고_출고를_중지한다() throws Exception {
        mockMvc.perform(patch("/api/warehouses/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/warehouses/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WH-01"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }
}
```

`src/test/java/com/sshyuny/omswms/coupon/CouponApiTest.java`:

```java
package com.sshyuny.omswms.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class CouponApiTest extends IntegrationTest {

    @Test
    void 쿠폰을_등록한다() throws Exception {
        String response = postJson("/api/coupons", """
                {"code": "WELCOME5000", "discountAmount": 5000, "minOrderAmount": 30000}
                """, status().isCreated());

        DocumentContext json = JsonPath.parse(response);
        assertThat(json.read("$.code", String.class)).isEqualTo("WELCOME5000");
        assertThat(json.read("$.discountAmount", Long.class)).isEqualTo(5_000L);
        assertThat(json.read("$.minOrderAmount", Long.class)).isEqualTo(30_000L);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.warehouse.*' --tests 'com.sshyuny.omswms.coupon.*'`
Expected: FAIL (404)

- [ ] **Step 3: 창고 구현**

`warehouse/WarehouseStatus.java`:

```java
package com.sshyuny.omswms.warehouse;

public enum WarehouseStatus {
    ACTIVE,
    SUSPENDED
}
```

`warehouse/Warehouse.java`:

```java
package com.sshyuny.omswms.warehouse;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warehouse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WarehouseStatus status;
}
```

`warehouse/WarehouseRepository.java`:

```java
package com.sshyuny.omswms.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}
```

`warehouse/WarehouseStatusChangeRequest.java`:

```java
package com.sshyuny.omswms.warehouse;

import jakarta.validation.constraints.NotNull;

public record WarehouseStatusChangeRequest(@NotNull WarehouseStatus status) {
}
```

`warehouse/WarehouseResponse.java`:

```java
package com.sshyuny.omswms.warehouse;

public record WarehouseResponse(Long id, String code, String name, WarehouseStatus status) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(), warehouse.getStatus());
    }
}
```

`warehouse/WarehouseService.java`:

```java
package com.sshyuny.omswms.warehouse;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseResponse changeStatus(Long id, WarehouseStatusChangeRequest request) {
        Warehouse warehouse = getWarehouseEntity(id);
        warehouse.setStatus(request.status());
        return WarehouseResponse.from(warehouse);
    }

    public WarehouseResponse getWarehouse(Long id) {
        return WarehouseResponse.from(getWarehouseEntity(id));
    }

    private Warehouse getWarehouseEntity(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }
}
```

`warehouse/WarehouseController.java`:

```java
package com.sshyuny.omswms.warehouse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/{id}")
    public WarehouseResponse getWarehouse(@PathVariable Long id) {
        return warehouseService.getWarehouse(id);
    }

    @PatchMapping("/{id}/status")
    public WarehouseResponse changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody WarehouseStatusChangeRequest request) {
        return warehouseService.changeStatus(id, request);
    }
}
```

- [ ] **Step 4: 쿠폰 구현**

`coupon/Coupon.java`:

```java
package com.sshyuny.omswms.coupon;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Long minOrderAmount;

    public Coupon(String code, Long discountAmount, Long minOrderAmount) {
        this.code = code;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
    }
}
```

`coupon/CouponRepository.java`:

```java
package com.sshyuny.omswms.coupon;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);
}
```

`coupon/CouponCreateRequest.java`:

```java
package com.sshyuny.omswms.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CouponCreateRequest(
        @NotBlank String code,
        @NotNull @Positive Long discountAmount,
        @NotNull @PositiveOrZero Long minOrderAmount
) {
}
```

`coupon/CouponResponse.java`:

```java
package com.sshyuny.omswms.coupon;

public record CouponResponse(Long id, String code, Long discountAmount, Long minOrderAmount) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getDiscountAmount(), coupon.getMinOrderAmount());
    }
}
```

`coupon/CouponService.java`:

```java
package com.sshyuny.omswms.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon = couponRepository.save(
                new Coupon(request.code(), request.discountAmount(), request.minOrderAmount()));
        return CouponResponse.from(coupon);
    }
}
```

`coupon/CouponController.java`:

```java
package com.sshyuny.omswms.coupon;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add src
git commit -m "feat: 창고 출고 상태 변경과 쿠폰 등록 API 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 4: 주문 생성·조회

**Files:**
- Create: `src/main/java/com/sshyuny/omswms/order/{Order,OrderLine,OrderStatus,OrderRepository,OrderService,OrderController,OrderCreateRequest,OrderLineRequest,OrderResponse,OrderLineResponse}.java`
- Modify: `src/test/java/com/sshyuny/omswms/IntegrationTest.java` (헬퍼 추가)
- Test: `src/test/java/com/sshyuny/omswms/order/OrderApiTest.java`

**Interfaces:**
- Consumes: `ProductRepository`, `CouponRepository#findByCode`, `StockService#decrease`
- Produces: `OrderRepository extends JpaRepository<Order, Long>`, `OrderStatus.{PENDING_PAYMENT, PAID, SHIPPED, CANCELLED}`, `Order#getId/getStatus/setStatus/getPaymentAmount/setPaidAt/setCancelledAt/getLines`, `OrderLine#getProductId/getQuantity`, `OrderResponse.from(Order)`, 테스트 헬퍼 `createOrder(Long productId, int quantity): Long`

- [ ] **Step 1: 테스트 헬퍼 추가**

`IntegrationTest` 클래스 끝에:

```java
    protected Long createOrder(Long productId, int quantity) throws Exception {
        String response = postJson("/api/orders", """
                {"customerId": 1, "lines": [{"productId": %d, "quantity": %d}]}
                """.formatted(productId, quantity), status().isCreated());
        return JsonPath.parse(response).read("$.id", Long.class);
    }
```

- [ ] **Step 2: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/order/OrderApiTest.java`:

```java
package com.sshyuny.omswms.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class OrderApiTest extends IntegrationTest {

    @Test
    void 주문하면_결제대기_상태가_되고_재고가_차감된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);

        Long orderId = createOrder(productId, 2);

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalProductAmount").value(20_000))
                .andExpect(jsonPath("$.shippingFee").value(3_000))
                .andExpect(jsonPath("$.couponDiscountAmount").value(0))
                .andExpect(jsonPath("$.paymentAmount").value(23_000))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].productId").value(productId))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(10_000))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].lineAmount").value(20_000));

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(8));
    }

    @Test
    void 상품금액이_5만원_이상이면_배송비가_무료이고_쿠폰_할인이_적용된다() throws Exception {
        Long shirtId = createProduct("SKU-SHIRT", 30_000L);
        Long pantsId = createProduct("SKU-PANTS", 25_000L);
        setStock(shirtId, 10);
        setStock(pantsId, 10);
        postJson("/api/coupons", """
                {"code": "WELCOME5000", "discountAmount": 5000, "minOrderAmount": 50000}
                """, status().isCreated());

        postJson("/api/orders", """
                {
                  "customerId": 1,
                  "lines": [
                    {"productId": %d, "quantity": 1},
                    {"productId": %d, "quantity": 1}
                  ],
                  "couponCode": "WELCOME5000"
                }
                """.formatted(shirtId, pantsId), status().isCreated());

        mockMvc.perform(get("/api/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductAmount").value(55_000))
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.couponCode").value("WELCOME5000"))
                .andExpect(jsonPath("$.couponDiscountAmount").value(5_000))
                .andExpect(jsonPath("$.paymentAmount").value(50_000))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }
}
```

(테이블을 매 테스트 `truncate`하므로 auto_increment가 초기화되어 첫 주문 ID는 1이다)

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.order.*'`
Expected: FAIL (`/api/orders` 404)

- [ ] **Step 4: 주문 구현** (⚠️ `createOrder`의 재고 차감 시점은 단계 2의 심어 둔 한계)

`order/OrderStatus.java`:

```java
package com.sshyuny.omswms.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    CANCELLED
}
```

`order/Order.java`:

```java
package com.sshyuny.omswms.order;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Long totalProductAmount;

    @Column(nullable = false)
    private Long shippingFee;

    @Column(length = 50)
    private String couponCode;

    @Column(nullable = false)
    private Long couponDiscountAmount;

    @Column(nullable = false)
    private Long paymentAmount;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    public Order(Long customerId, LocalDateTime orderedAt) {
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.orderedAt = orderedAt;
    }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
}
```

`order/OrderLine.java`:

```java
package com.sshyuny.omswms.order;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long lineAmount;

    public OrderLine(Long productId, String productName, Long unitPrice, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineAmount = unitPrice * quantity;
    }
}
```

`order/OrderRepository.java`:

```java
package com.sshyuny.omswms.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

`order/OrderLineRequest.java`:

```java
package com.sshyuny.omswms.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {
}
```

`order/OrderCreateRequest.java`:

```java
package com.sshyuny.omswms.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequest(
        @NotNull Long customerId,
        @NotEmpty @Valid List<OrderLineRequest> lines,
        String couponCode
) {
}
```

`order/OrderLineResponse.java`:

```java
package com.sshyuny.omswms.order;

public record OrderLineResponse(
        Long productId,
        String productName,
        Long unitPrice,
        Integer quantity,
        Long lineAmount
) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getProductId(),
                line.getProductName(),
                line.getUnitPrice(),
                line.getQuantity(),
                line.getLineAmount());
    }
}
```

`order/OrderResponse.java`:

```java
package com.sshyuny.omswms.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        Long totalProductAmount,
        Long shippingFee,
        String couponCode,
        Long couponDiscountAmount,
        Long paymentAmount,
        LocalDateTime orderedAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        List<OrderLineResponse> lines
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalProductAmount(),
                order.getShippingFee(),
                order.getCouponCode(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount(),
                order.getOrderedAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
```

`order/OrderService.java`:

```java
package com.sshyuny.omswms.order;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.coupon.Coupon;
import com.sshyuny.omswms.coupon.CouponRepository;
import com.sshyuny.omswms.product.Product;
import com.sshyuny.omswms.product.ProductRepository;
import com.sshyuny.omswms.stock.StockService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final long SHIPPING_FEE = 3_000L;
    private static final long FREE_SHIPPING_THRESHOLD = 50_000L;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final StockService stockService;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        Order order = new Order(request.customerId(), LocalDateTime.now());

        for (OrderLineRequest lineRequest : request.lines()) {
            Product product = productRepository.findById(lineRequest.productId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            stockService.decrease(product.getId(), lineRequest.quantity());
            order.addLine(new OrderLine(product.getId(), product.getName(), product.getPrice(), lineRequest.quantity()));
        }

        long totalProductAmount = order.getLines().stream()
                .mapToLong(OrderLine::getLineAmount)
                .sum();
        long shippingFee = totalProductAmount >= FREE_SHIPPING_THRESHOLD ? 0L : SHIPPING_FEE;
        long couponDiscountAmount = calculateCouponDiscount(request.couponCode(), totalProductAmount);

        order.setTotalProductAmount(totalProductAmount);
        order.setShippingFee(shippingFee);
        order.setCouponCode(request.couponCode());
        order.setCouponDiscountAmount(couponDiscountAmount);
        order.setPaymentAmount(totalProductAmount + shippingFee - couponDiscountAmount);

        orderRepository.save(order);
        return OrderResponse.from(order);
    }

    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(getOrderEntity(id));
    }

    private long calculateCouponDiscount(String couponCode, long totalProductAmount) {
        if (couponCode == null) {
            return 0L;
        }
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        if (totalProductAmount < coupon.getMinOrderAmount()) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }
        return Math.min(coupon.getDiscountAmount(), totalProductAmount);
    }

    private Order getOrderEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
```

`order/OrderController.java`:

```java
package com.sshyuny.omswms.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add src
git commit -m "feat: 주문 생성과 조회 API 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 5: 결제 승인과 출고지시 생성

**Files:**
- Create: `src/main/java/com/sshyuny/omswms/payment/{Payment,PaymentStatus,PaymentRepository,PaymentService,PaymentController,PaymentResponse,FakePgClient,PgTransaction,PgTransactionStatus}.java`
- Create: `src/main/java/com/sshyuny/omswms/shipment/{Shipment,ShipmentStatus,ShipmentRepository,ShipmentService,ShipmentController,ShipmentResponse}.java`
- Modify: `src/test/java/com/sshyuny/omswms/IntegrationTest.java` (헬퍼 추가)
- Test: `src/test/java/com/sshyuny/omswms/payment/PaymentApiTest.java`

**Interfaces:**
- Consumes: `OrderRepository`, `OrderStatus`, `WarehouseRepository`, `WarehouseStatus`
- Produces: `FakePgClient#approve(Long amount): String`, `FakePgClient#cancel(String transactionId)`, `FakePgClient#findTransaction(String): Optional<PgTransaction>`, `PaymentService#cancel(Long orderId)`, `ShipmentRepository#findByOrderId(Long): Optional<Shipment>`, `ShipmentStatus.{READY, PICKING, PACKED, SHIPPED, CANCELLED}`, `Shipment#getStatus/setStatus`, 테스트 헬퍼 `pay(Long orderId): String`(PG 거래 ID 반환)

- [ ] **Step 1: 테스트 헬퍼 추가**

`IntegrationTest` 클래스 끝에:

```java
    protected String pay(Long orderId) throws Exception {
        String response = postJson("/api/orders/" + orderId + "/payment", "", status().isCreated());
        return JsonPath.parse(response).read("$.pgTransactionId", String.class);
    }
```

- [ ] **Step 2: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/payment/PaymentApiTest.java`:

```java
package com.sshyuny.omswms.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentApiTest extends IntegrationTest {

    @Autowired
    private FakePgClient fakePgClient;

    @Test
    void 결제하면_주문이_결제완료되고_출고지시가_생성된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 2);

        String response = postJson("/api/orders/" + orderId + "/payment", "", status().isCreated());

        DocumentContext payment = JsonPath.parse(response);
        assertThat(payment.read("$.orderId", Long.class)).isEqualTo(orderId);
        assertThat(payment.read("$.amount", Long.class)).isEqualTo(23_000L);
        assertThat(payment.read("$.status", String.class)).isEqualTo("APPROVED");

        String pgTransactionId = payment.read("$.pgTransactionId", String.class);
        assertThat(fakePgClient.findTransaction(pgTransactionId))
                .hasValueSatisfying(transaction -> {
                    assertThat(transaction.amount()).isEqualTo(23_000L);
                    assertThat(transaction.status()).isEqualTo(PgTransactionStatus.APPROVED);
                });

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        mockMvc.perform(get("/api/shipments").param("orderId", String.valueOf(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.status").value("READY"));
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.payment.*'`
Expected: FAIL (컴파일 오류: `FakePgClient` 없음)

- [ ] **Step 4: 가짜 PG 구현**

`payment/PgTransactionStatus.java`:

```java
package com.sshyuny.omswms.payment;

public enum PgTransactionStatus {
    APPROVED,
    CANCELLED
}
```

`payment/PgTransaction.java`:

```java
package com.sshyuny.omswms.payment;

public record PgTransaction(String transactionId, Long amount, PgTransactionStatus status) {
}
```

`payment/FakePgClient.java`:

```java
package com.sshyuny.omswms.payment;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 PG사 결제 API를 대신하는 가짜 클라이언트.
 * 승인·취소 내역은 PG사 쪽 저장소를 흉내 내어 메모리에 보관한다.
 */
@Slf4j
@Component
public class FakePgClient {

    private final Map<String, PgTransaction> transactions = new ConcurrentHashMap<>();

    public String approve(Long amount) {
        String transactionId = UUID.randomUUID().toString();
        transactions.put(transactionId, new PgTransaction(transactionId, amount, PgTransactionStatus.APPROVED));
        log.info("fake pg: approved transactionId={}, amount={}", transactionId, amount);
        return transactionId;
    }

    public void cancel(String transactionId) {
        PgTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("존재하지 않는 PG 거래입니다. transactionId=" + transactionId);
        }
        transactions.put(transactionId,
                new PgTransaction(transactionId, transaction.amount(), PgTransactionStatus.CANCELLED));
        log.info("fake pg: cancelled transactionId={}", transactionId);
    }

    public Optional<PgTransaction> findTransaction(String transactionId) {
        return Optional.ofNullable(transactions.get(transactionId));
    }
}
```

- [ ] **Step 5: 출고지시 구현** (⚠️ 창고를 설정값 하나로 고정하는 것은 단계 4의 심어 둔 한계)

`shipment/ShipmentStatus.java`:

```java
package com.sshyuny.omswms.shipment;

public enum ShipmentStatus {
    READY,
    PICKING,
    PACKED,
    SHIPPED,
    CANCELLED
}
```

`shipment/Shipment.java`:

```java
package com.sshyuny.omswms.shipment;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    private LocalDateTime shippedAt;

    public Shipment(Long orderId, Long warehouseId) {
        this.orderId = orderId;
        this.warehouseId = warehouseId;
        this.status = ShipmentStatus.READY;
    }
}
```

`shipment/ShipmentRepository.java`:

```java
package com.sshyuny.omswms.shipment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);
}
```

`shipment/ShipmentResponse.java`:

```java
package com.sshyuny.omswms.shipment;

import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        Long orderId,
        Long warehouseId,
        ShipmentStatus status,
        LocalDateTime shippedAt
) {

    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getWarehouseId(),
                shipment.getStatus(),
                shipment.getShippedAt());
    }
}
```

`shipment/ShipmentService.java`:

```java
package com.sshyuny.omswms.shipment;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.warehouse.Warehouse;
import com.sshyuny.omswms.warehouse.WarehouseRepository;
import com.sshyuny.omswms.warehouse.WarehouseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final WarehouseRepository warehouseRepository;

    @Value("${app.default-warehouse-id}")
    private Long defaultWarehouseId;

    @Transactional
    public void createShipment(Long orderId) {
        Warehouse warehouse = warehouseRepository.findById(defaultWarehouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (warehouse.getStatus() == WarehouseStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.WAREHOUSE_SUSPENDED);
        }
        shipmentRepository.save(new Shipment(orderId, warehouse.getId()));
    }

    public ShipmentResponse getShipment(Long id) {
        return ShipmentResponse.from(getShipmentEntity(id));
    }

    public ShipmentResponse getShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND));
        return ShipmentResponse.from(shipment);
    }

    private Shipment getShipmentEntity(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND));
    }
}
```

`shipment/ShipmentController.java`:

```java
package com.sshyuny.omswms.shipment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/{id}")
    public ShipmentResponse getShipment(@PathVariable Long id) {
        return shipmentService.getShipment(id);
    }

    @GetMapping
    public ShipmentResponse getShipmentByOrderId(@RequestParam Long orderId) {
        return shipmentService.getShipmentByOrderId(orderId);
    }
}
```

- [ ] **Step 6: 결제 구현** (⚠️ `approve`가 한 트랜잭션인 것은 단계 3의 심어 둔 한계)

`payment/PaymentStatus.java`:

```java
package com.sshyuny.omswms.payment;

public enum PaymentStatus {
    APPROVED,
    CANCELLED
}
```

`payment/Payment.java`:

```java
package com.sshyuny.omswms.payment;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 64)
    private String pgTransactionId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime approvedAt;

    private LocalDateTime cancelledAt;

    public Payment(Long orderId, String pgTransactionId, Long amount, LocalDateTime approvedAt) {
        this.orderId = orderId;
        this.pgTransactionId = pgTransactionId;
        this.amount = amount;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt;
    }
}
```

`payment/PaymentRepository.java`:

```java
package com.sshyuny.omswms.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
}
```

`payment/PaymentResponse.java`:

```java
package com.sshyuny.omswms.payment;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String pgTransactionId,
        Long amount,
        PaymentStatus status,
        LocalDateTime approvedAt,
        LocalDateTime cancelledAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPgTransactionId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getApprovedAt(),
                payment.getCancelledAt());
    }
}
```

`payment/PaymentService.java`:

```java
package com.sshyuny.omswms.payment;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.order.Order;
import com.sshyuny.omswms.order.OrderRepository;
import com.sshyuny.omswms.order.OrderStatus;
import com.sshyuny.omswms.shipment.ShipmentService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ShipmentService shipmentService;
    private final FakePgClient fakePgClient;

    @Transactional
    public PaymentResponse approve(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        LocalDateTime now = LocalDateTime.now();
        String pgTransactionId = fakePgClient.approve(order.getPaymentAmount());
        Payment payment = paymentRepository.save(
                new Payment(order.getId(), pgTransactionId, order.getPaymentAmount(), now));

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        shipmentService.createShipment(order.getId());

        return PaymentResponse.from(payment);
    }

    @Transactional
    public void cancel(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        fakePgClient.cancel(payment.getPgTransactionId());
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(LocalDateTime.now());
    }
}
```

`payment/PaymentController.java`:

```java
package com.sshyuny.omswms.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/orders/{orderId}/payment")
    public ResponseEntity<PaymentResponse> approve(@PathVariable Long orderId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.approve(orderId));
    }
}
```

- [ ] **Step 7: 통과 확인**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add src
git commit -m "feat: 결제 승인과 출고지시 생성 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 6: 출고 진행 (피킹 → 패킹 → 출고완료)

**Files:**
- Modify: `src/main/java/com/sshyuny/omswms/shipment/ShipmentService.java`, `src/main/java/com/sshyuny/omswms/shipment/ShipmentController.java`
- Modify: `src/test/java/com/sshyuny/omswms/IntegrationTest.java` (헬퍼 추가)
- Test: `src/test/java/com/sshyuny/omswms/shipment/ShipmentApiTest.java`

**Interfaces:**
- Consumes: `OrderRepository`, `OrderStatus.SHIPPED`
- Produces: `POST /api/shipments/{id}/picking|packing|shipping`, 테스트 헬퍼 `shipmentIdOf(Long orderId): Long`

- [ ] **Step 1: 테스트 헬퍼 추가**

`IntegrationTest`에 import `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get` 추가 후 클래스 끝에:

```java
    protected Long shipmentIdOf(Long orderId) throws Exception {
        String response = mockMvc.perform(get("/api/shipments").param("orderId", String.valueOf(orderId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.parse(response).read("$.id", Long.class);
    }
```

- [ ] **Step 2: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/shipment/ShipmentApiTest.java`:

```java
package com.sshyuny.omswms.shipment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class ShipmentApiTest extends IntegrationTest {

    @Test
    void 피킹_패킹_출고를_거치면_주문이_출고완료된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 1);
        pay(orderId);
        Long shipmentId = shipmentIdOf(orderId);

        mockMvc.perform(post("/api/shipments/{id}/picking", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKING"));
        mockMvc.perform(post("/api/shipments/{id}/packing", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"));
        mockMvc.perform(post("/api/shipments/{id}/shipping", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shippedAt").isNotEmpty());

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.shipment.*'`
Expected: FAIL (`/picking` 404 또는 405)

- [ ] **Step 4: 출고 상태 변경 구현**

`ShipmentService`에 필드 `private final OrderRepository orderRepository;` 추가(import `com.sshyuny.omswms.order.Order`, `com.sshyuny.omswms.order.OrderRepository`, `com.sshyuny.omswms.order.OrderStatus`, `java.time.LocalDateTime`), `getShipmentEntity` 위에 메서드 추가:

```java
    @Transactional
    public ShipmentResponse startPicking(Long id) {
        Shipment shipment = getShipmentEntity(id);
        changeStatus(shipment, ShipmentStatus.READY, ShipmentStatus.PICKING);
        return ShipmentResponse.from(shipment);
    }

    @Transactional
    public ShipmentResponse pack(Long id) {
        Shipment shipment = getShipmentEntity(id);
        changeStatus(shipment, ShipmentStatus.PICKING, ShipmentStatus.PACKED);
        return ShipmentResponse.from(shipment);
    }

    @Transactional
    public ShipmentResponse ship(Long id) {
        Shipment shipment = getShipmentEntity(id);
        changeStatus(shipment, ShipmentStatus.PACKED, ShipmentStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());

        Order order = orderRepository.findById(shipment.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.setStatus(OrderStatus.SHIPPED);

        return ShipmentResponse.from(shipment);
    }

    private void changeStatus(Shipment shipment, ShipmentStatus expected, ShipmentStatus next) {
        if (shipment.getStatus() != expected) {
            throw new BusinessException(ErrorCode.INVALID_SHIPMENT_STATUS);
        }
        shipment.setStatus(next);
    }
```

`ShipmentController`에 import `org.springframework.web.bind.annotation.PostMapping` 추가 후:

```java
    @PostMapping("/{id}/picking")
    public ShipmentResponse startPicking(@PathVariable Long id) {
        return shipmentService.startPicking(id);
    }

    @PostMapping("/{id}/packing")
    public ShipmentResponse pack(@PathVariable Long id) {
        return shipmentService.pack(id);
    }

    @PostMapping("/{id}/shipping")
    public ShipmentResponse ship(@PathVariable Long id) {
        return shipmentService.ship(id);
    }
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add src
git commit -m "feat: 출고 진행 상태 변경 API 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 7: 주문 전체 취소

**Files:**
- Modify: `src/main/java/com/sshyuny/omswms/order/OrderService.java`, `src/main/java/com/sshyuny/omswms/order/OrderController.java`
- Test: `src/test/java/com/sshyuny/omswms/order/OrderCancelApiTest.java`

**Interfaces:**
- Consumes: `ShipmentRepository#findByOrderId`, `ShipmentStatus`, `PaymentService#cancel`, `StockService#increase`, `FakePgClient#findTransaction`
- Produces: `POST /api/orders/{id}/cancel`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/test/java/com/sshyuny/omswms/order/OrderCancelApiTest.java`:

```java
package com.sshyuny.omswms.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import com.sshyuny.omswms.payment.FakePgClient;
import com.sshyuny.omswms.payment.PgTransaction;
import com.sshyuny.omswms.payment.PgTransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderCancelApiTest extends IntegrationTest {

    @Autowired
    private FakePgClient fakePgClient;

    @Test
    void 결제_전_주문을_취소하면_재고가_복구된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 3);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void 출고_준비_중인_주문을_취소하면_결제와_출고지시도_취소된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 3);
        String pgTransactionId = pay(orderId);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/shipments").param("orderId", String.valueOf(orderId)))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(10));
        assertThat(fakePgClient.findTransaction(pgTransactionId))
                .map(PgTransaction::status)
                .hasValue(PgTransactionStatus.CANCELLED);
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests 'com.sshyuny.omswms.order.OrderCancelApiTest'`
Expected: FAIL (`/cancel` 404)

- [ ] **Step 3: 취소 구현** (⚠️ 출고 상태 확인 후 변경까지 락이 없는 것은 단계 5의 심어 둔 한계)

`OrderService`에 필드 추가(import `com.sshyuny.omswms.payment.PaymentService`, `com.sshyuny.omswms.shipment.Shipment`, `com.sshyuny.omswms.shipment.ShipmentRepository`, `com.sshyuny.omswms.shipment.ShipmentStatus`):

```java
    private final ShipmentRepository shipmentRepository;
    private final PaymentService paymentService;
```

`getOrder` 아래에 메서드 추가:

```java
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = getOrderEntity(id);

        if (order.getStatus() == OrderStatus.PAID) {
            Shipment shipment = shipmentRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND));
            if (shipment.getStatus() != ShipmentStatus.READY) {
                throw new BusinessException(ErrorCode.INVALID_SHIPMENT_STATUS);
            }
            shipment.setStatus(ShipmentStatus.CANCELLED);
            paymentService.cancel(order.getId());
        } else if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        for (OrderLine line : order.getLines()) {
            stockService.increase(line.getProductId(), line.getQuantity());
        }
        return OrderResponse.from(order);
    }
```

`OrderController`에:

```java
    @PostMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew test`
Expected: PASS (전체)

- [ ] **Step 5: 커밋**

```bash
git add src
git commit -m "feat: 주문 전체 취소 API 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 8: 숨김 파일 (DO-NOT-READ/stage-1 ~ stage-6)

**Files:**
- Create: `DO-NOT-READ/stage-1.md` ~ `DO-NOT-READ/stage-6.md`

각 파일 맨 위에 `> 단계 N 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.`를 넣는다. 구현이 계획과 달라졌다면 **실제 코드 기준으로** 클래스·메서드 이름을 고쳐 적는다.

- [ ] **Step 1: `stage-1.md` 작성**

```markdown
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
```

- [ ] **Step 2: `stage-2.md` 작성**

```markdown
# 단계 2 — 결제 안 하고 이탈한 주문 때문에 재고가 있는데도 품절로 나옴

> 단계 2 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.

## 심어 둔 원인
- 위치: `OrderService.createOrder`가 주문 생성 시점에 `StockService.decrease`로 재고를 바로 뺀다.
- `PENDING_PAYMENT` 주문은 만료되지 않는다. 사용자가 직접 취소하지 않으면 재고가 영원히 묶인다.
- 재고는 `stock.quantity` 컬럼 하나뿐이다. 실물재고(on-hand)와 예약(reserved), 가용재고(ATP)가 구분되지 않는다. `StockResponse.soldOut`도 이 값 하나로 판단한다.
- 부수 함정: 서비스 곳곳에서 `LocalDateTime.now()`를 직접 호출한다. 만료 같은 시간 기반 로직을 테스트하려면 시간 주입(`Clock` 등) 구조로 먼저 바꿔야 한다.

## 해법 방향 (참고)
- 예약(reservation) 개념 도입: 주문 시 예약 생성(만료 시각 포함), 결제 시 확정, 만료 시 해제
- 가용재고 = 실물재고 − 예약 (− 안전재고). 계산 규칙은 정책 객체로 분리
- 만료 처리: 스케줄러 일괄 처리 또는 조회 시점 지연 만료

## 리뷰 체크포인트
- 만료 처리와 결제 승인의 경합 (만료된 주문을 결제하는 경우)
- 만료 처리의 멱등성 (두 번 돌아도 재고가 두 번 복구되지 않는지)
- 단계 1의 동시성 해법이 예약 모델에서도 유지되는지
```

- [ ] **Step 3: `stage-3.md` 작성**

```markdown
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
```

- [ ] **Step 4: `stage-4.md` 작성**

```markdown
# 단계 4 — 두 번째 창고가 생김. 한 창고 재고로 부족하면 나눠서 출고

> 단계 4 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.

## 심어 둔 원인
- 창고: `ShipmentService`가 `app.default-warehouse-id` 설정값 하나로 출고 창고를 고정한다.
- 재고: `stock`은 상품당 1행(`uk_stock_product_id`). 창고 차원이 없다.
- 출고: `shipment.order_id`가 unique. 주문 1건 = 출고 1건. 출고 라인(어떤 상품 몇 개)도 없다.
- 금액: 배송비·쿠폰 할인이 주문 단위로만 있고 라인·출고 단위 배분이 없다.
- 취소 시 재고 복구도 상품 단위로만 한다.

## 해법 방향 (참고)
- 재고를 (상품, 창고) 단위로 바꾸는 Flyway 마이그레이션 + 기존 데이터 이전
- 할당 정책: 한 창고에서 전량 가능하면 그 창고, 아니면 분할 (정책 객체로)
- 출고를 주문당 N건 + 출고 라인으로
- 쿠폰 할인·배송비 배분: 비율 배분 + 끝전 처리 (합계가 원래 금액과 정확히 일치해야 함)

## 리뷰 체크포인트
- 배분 금액 합계 = 원래 금액 (반올림 오차)
- 분할 출고 시 배송비 정책을 명시했는지 (추가 배송비를 누가 부담하는지)
- 기존 테스트(단일 창고 흐름)가 유지되는지
```

- [ ] **Step 5: `stage-5.md` 작성**

```markdown
# 단계 5 — 피킹이 진행 중인 주문에서 일부 상품만 취소

> 단계 5 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.

## 심어 둔 원인
- 위치: `OrderService.cancelOrder`
- 전체 취소만 있고, 출고가 `READY`일 때만 가능하다. 라인 단위 상태가 없다.
- 출고 상태를 읽고(`READY` 확인) 바꾸기(`CANCELLED`)까지 락·버전이 없다. 같은 순간 `ShipmentService.startPicking`이 `PICKING`으로 바꾸면 둘 다 커밋되고 나중 커밋이 이긴다. 주문·결제는 취소됐는데 출고는 피킹 중인 상태가 생길 수 있다.
- `FakePgClient.cancel`은 전액 취소만 지원한다. 부분 환불 불가.
- PG 취소가 DB 커밋 전에 일어난다. 이후 단계에서 실패하면 PG만 취소된 상태가 남는다 (단계 3과 같은 계열).

## 해법 방향 (참고)
- 출고에 낙관적 락(`@Version`) 또는 상태 전이를 조건부 update로
- 주문 라인 단위 취소 상태와 "취소 요청 → 창고 확인 → 확정" 흐름 (피킹 중이면 창고가 수락/거절)
- 부분 환불 금액 계산: 단계 4의 라인별 배분 금액 기준
- `FakePgClient`에 부분 취소 추가

## 리뷰 체크포인트
- 상태 전이가 명시적으로 정의돼 있는지 (허용되지 않은 전이 차단)
- 동시성 테스트로 경합을 재현했는지
- 부분 취소 후 남은 쿠폰 최소 주문 금액 조건 처리
```

- [ ] **Step 6: `stage-6.md` 작성**

```markdown
# 단계 6 — 반품, 검수, 재입고, 부분환불

> 단계 6 작업이 끝난 뒤에 연다. 리뷰어(AI)는 사용자가 이 단계의 리뷰·힌트·공개를 요청했을 때만 읽는다.

## 심어 둔 원인
- 출고완료(`SHIPPED`) 이후의 흐름이 전혀 없다. 주문 `SHIPPED`가 종착 상태다.
- 반품 엔티티·상태가 없고, 재고 증가는 취소 경로에만 있다.
- 양품/불량 구분이 없다.
- PG는 전액 취소만 지원한다 (단계 5에서 바꿨다면 그 구조 위에서).

## 해법 방향 (참고)
- 반품 요청 → 회수 → 검수(양품 재입고 / 불량 폐기) → 환불 확정
- 반품 배송비 차감 정책
- 재고 이동을 원장(ledger)으로 기록하면 추적이 쉬워짐

## 리뷰 체크포인트
- 검수 결과별 재고 반영이 정확한지
- 부분 반품 시 환불 금액 = 배분 금액 기준인지
```

- [ ] **Step 7: 커밋**

```bash
git add DO-NOT-READ
git commit -m "docs: 단계별 숨김 파일 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 9: README와 숨김 장치 (.ignore, AGENTS.md, CLAUDE.md, ask 권한)

**Files:**
- Create/Modify: `README.md`, `.ignore`, `AGENTS.md`, `CLAUDE.md`, `.claude/settings.json`

- [ ] **Step 1: `README.md` 작성**

````markdown
# oms-wms-lab

주문(OMS)과 창고(WMS)가 만나는 지점의 문제를 단계별로 풀어 보는 개인 프로젝트.

## 이 리포를 읽는 방법

- **base(첫 커밋)는 의도적으로 단순하게 만든 출발점이다.** "이미 돌아가는 회사 프로젝트에 합류한 상황"을 재현하려고 AI(Claude)에게 만들게 했다. base 코드는 내 작업이 아니다.
- 이후 각 단계는 GitHub Issue(티켓) 하나와 PR 하나로 진행했고, 그 커밋들이 내 작업이다.
- `DO-NOT-READ/`에는 base를 만들 때 미리 적어 둔 단계별 원인이 들어 있다. 나는 단계를 끝낸 뒤에만 해당 파일을 열어 내 진단과 비교했다.

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

(단계를 진행하며 작성)
````

- [ ] **Step 2: `.ignore` 작성**

```
DO-NOT-READ/
```

- [ ] **Step 3: `AGENTS.md` 작성**

```markdown
# AI 에이전트 작업 규칙

이 리포는 사용자가 단계별 티켓을 직접 해결하는 학습·포트폴리오 프로젝트다. AI는 리뷰어 역할만 한다.

## DO-NOT-READ/ 폴더

- `DO-NOT-READ/`에는 각 단계의 원인(스포일러)과 base 구현 계획이 들어 있다. **기본적으로 읽지 않는다.** 검색·목록 결과에 보여도 열지 않는다.
- **예외**: 사용자가 특정 단계의 리뷰·힌트·공개를 명시적으로 요청했을 때, **그 단계 파일(`DO-NOT-READ/stage-N.md`)만** 읽는다. 다른 단계 파일과 `base-plan.md`는 읽지 않는다.
- 읽은 내용은 사용자가 요청한 힌트 수준을 넘어서 말하지 않는다.

## 역할

- 단계 작업(재현 테스트, 원인 분석, 수정)은 사용자가 한다. **AI는 해법 코드를 쓰지 않는다.**
- 리뷰 기준: 사용자 해법이 숨김 파일의 의도와 달라도 된다. 증상을 해결하는지, 부작용이 없는지만 본다.

## 힌트 규칙

사용자가 요청할 때만, 요청한 수준까지만 준다. 해법 코드는 어느 수준에서도 쓰지 않는다.

| 수준 | AI가 말하는 것 |
|---|---|
| 1 | 가설이 맞는지 틀린지만 |
| 2 | 어느 영역을 봐야 하는지 |
| 3 | 원인 설명 |

## 기타

- 이 리포 밖의 다른 프로젝트 코드나 컨벤션을 참고하지 않는다.
```

- [ ] **Step 4: `CLAUDE.md` 작성**

```markdown
@AGENTS.md
```

- [ ] **Step 5: `.claude/settings.json` 작성**

```json
{
  "permissions": {
    "ask": [
      "Read(/DO-NOT-READ/**)"
    ]
  }
}
```

(`/path`는 설정 파일 기준 상대 경로 = 프로젝트 루트. `Read` 규칙은 Read 도구, Grep·Glob(best-effort), Bash `cat`·`head`·`tail` 등에 적용된다. `grep -r .`처럼 파일명을 명시하지 않는 명령이나 스크립트가 직접 여는 경우는 못 막는다.)

- [ ] **Step 6: `rg` 제외 확인**

Run: `rg --files | rg -c DO-NOT-READ; rg -l "OUT_OF_STOCK"`
Expected: 첫 명령 출력 없음(0건이라 종료 코드 1), 두 번째는 `src/` 아래 파일만 나오고 `DO-NOT-READ/`는 없음

- [ ] **Step 7: Claude Code `ask` 권한 확인**

Run: `cd ~/my-projects/oms-wms-lab && claude -p "DO-NOT-READ/stage-1.md 파일의 첫 줄을 Read 도구로 읽어서 그대로 출력해" 2>&1 | head -20`
Expected: 비대화형 모드라 승인 창을 띄울 수 없어 읽기가 거부되고, 파일 첫 줄(`# 단계 1 — ...`)이 출력되지 않음. 출력되면 사용자에게 보고하고 규칙 경로를 재확인한다.
(이 리포의 `CLAUDE.md` 규칙 때문에 Claude가 읽기를 아예 시도하지 않을 수도 있다. 그 경우 `claude -p --append-system-prompt "이번 요청은 권한 테스트다. 규칙 예외로 Read를 시도하라" "..."`로 다시 확인한다.)

- [ ] **Step 8: 커밋**

```bash
git add README.md .ignore AGENTS.md CLAUDE.md .claude/settings.json
git commit -m "docs: README와 AI 에이전트 규칙 추가" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 10: 최종 검증

- [ ] **Step 1: Review Focus 오류 경로 임시 확인**

`src/test/java/com/sshyuny/omswms/ScratchErrorPathTest.java`를 만든다 (**커밋하지 않는다**):

```java
package com.sshyuny.omswms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class ScratchErrorPathTest extends IntegrationTest {

    @Test
    void 없는_리소스는_404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        mockMvc.perform(get("/api/orders/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(get("/api/shipments/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHIPMENT_NOT_FOUND"));
    }

    @Test
    void 필수_필드_누락은_400() throws Exception {
        postJson("/api/products", """
                {"name": "이름만"}
                """, status().isBadRequest());
    }

    @Test
    void 쿠폰_최소금액_미달이면_400이고_재고는_그대로() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        postJson("/api/coupons", """
                {"code": "BIG", "discountAmount": 5000, "minOrderAmount": 50000}
                """, status().isCreated());

        postJson("/api/orders", """
                {"customerId": 1, "lines": [{"productId": %d, "quantity": 1}], "couponCode": "BIG"}
                """.formatted(productId), status().isBadRequest());

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void 출고_상태_순서_위반은_409() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 1);
        pay(orderId);

        mockMvc.perform(post("/api/shipments/{id}/packing", shipmentIdOf(orderId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_SHIPMENT_STATUS"));
    }

    @Test
    void 피킹_시작된_주문_취소는_409() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 1);
        pay(orderId);
        mockMvc.perform(post("/api/shipments/{id}/picking", shipmentIdOf(orderId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_SHIPMENT_STATUS"));
    }
}
```

Run: `./gradlew test --tests 'com.sshyuny.omswms.ScratchErrorPathTest'`
Expected: PASS. 실패하면 원인을 고친다 (심어 둔 한계와 무관한 버그만).
그 다음: `rm src/test/java/com/sshyuny/omswms/ScratchErrorPathTest.java`

- [ ] **Step 2: 심어 둔 한계가 실제로 재현되는지 임시 확인 (단계 1, 3)**

같은 방식으로 임시 테스트를 만들어 확인하고 지운다 (**커밋하지 않는다**).
- 단계 1: 재고 1개, 스레드 10개 동시 주문 → 최종 재고 < 0 또는 성공 주문 > 1
- 단계 3: 창고 `SUSPENDED` → 결제 409 → `payment` 테이블 0건, 해당 금액의 PG `APPROVED` 거래 존재

재현되지 않으면 계획의 해당 코드를 다시 확인한다. 재현돼야 단계 티켓이 성립한다.

- [ ] **Step 3: 전체 테스트와 실행 확인**

Run: `git status --short` → 임시 테스트 파일이 남아 있지 않은지 확인
Run: `./gradlew clean test`
Expected: PASS
Run: `./gradlew bootTestRun` 백그라운드 실행 후 `curl -s localhost:8080/api/warehouses/1`
Expected: `{"id":1,"code":"WH-01","name":"기본 물류센터","status":"ACTIVE"}`. 확인 후 프로세스 종료.

- [ ] **Step 4: 마무리**

- 이 계획(`DO-NOT-READ/base-plan.md`)이 실제 구현과 다르면 차이를 계획 끝 "구현 메모"에 적고 커밋
- (a) PR 방식이면 `git push -u origin base` 후 `gh pr create --base main --head base`로 PR #1 생성 → 사용자에게 **squash merge** 요청
- 설계 문서 §8 진행 상황과 §10 체크리스트를 갱신 (base 완료, 다음은 단계 0 온보딩)
- 사용자에게 IntelliJ에서 `DO-NOT-READ/`를 Excluded로 지정하도록 안내
