package com.sshyuny.omswms.product;

public record ProductResponse(Long id, String skuCode, String name, Long price) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSkuCode(), product.getName(), product.getPrice());
    }
}
