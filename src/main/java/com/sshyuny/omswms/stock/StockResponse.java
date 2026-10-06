package com.sshyuny.omswms.stock;

public record StockResponse(Long productId, Integer quantity, boolean soldOut) {

    public static StockResponse from(Stock stock) {
        return new StockResponse(stock.getProductId(), stock.getQuantity(), stock.getQuantity() <= 0);
    }
}
