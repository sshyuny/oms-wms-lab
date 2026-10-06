package com.sshyuny.omswms.payment;

public record PgTransaction(String transactionId, Long amount, PgTransactionStatus status) {
}
