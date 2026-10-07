package com.sshyuny.omswms.order;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.coupon.Coupon;
import com.sshyuny.omswms.coupon.CouponRepository;
import com.sshyuny.omswms.payment.PaymentService;
import com.sshyuny.omswms.product.Product;
import com.sshyuny.omswms.product.ProductRepository;
import com.sshyuny.omswms.shipment.Shipment;
import com.sshyuny.omswms.shipment.ShipmentRepository;
import com.sshyuny.omswms.shipment.ShipmentStatus;
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
    private final ShipmentRepository shipmentRepository;
    private final PaymentService paymentService;

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
