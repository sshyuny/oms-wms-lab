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

        int decreaseQuantity = stockRepository.decreaseQuantity(productId, quantity);
        if (decreaseQuantity != 1) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
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
