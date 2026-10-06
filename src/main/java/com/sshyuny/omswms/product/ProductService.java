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
        if (productRepository.existsBySkuCode(request.skuCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU_CODE);
        }
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
