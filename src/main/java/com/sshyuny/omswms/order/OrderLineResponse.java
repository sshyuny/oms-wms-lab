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
