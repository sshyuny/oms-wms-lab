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
