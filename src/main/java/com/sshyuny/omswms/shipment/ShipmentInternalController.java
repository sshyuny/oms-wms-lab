package com.sshyuny.omswms.shipment;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/internal/shipments")
@RequiredArgsConstructor 
public class ShipmentInternalController {

    private final ShipmentService shipmentService;

    @PostMapping("/create")
    public Response createForPaidOrder() {
        List<Long> orderIds = shipmentService.createForPaidOrder();

        return new Response(orderIds);
    }

    record Response(List<Long> orderIds) {}
    
}
