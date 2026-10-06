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
