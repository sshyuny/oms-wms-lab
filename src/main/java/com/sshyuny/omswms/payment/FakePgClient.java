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
