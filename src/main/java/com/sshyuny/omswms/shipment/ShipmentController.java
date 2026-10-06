package com.sshyuny.omswms.shipment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/{id}")
    public ShipmentResponse getShipment(@PathVariable Long id) {
        return shipmentService.getShipment(id);
    }

    @GetMapping
    public ShipmentResponse getShipmentByOrderId(@RequestParam Long orderId) {
        return shipmentService.getShipmentByOrderId(orderId);
    }

    @PostMapping("/{id}/picking")
    public ShipmentResponse startPicking(@PathVariable Long id) {
        return shipmentService.startPicking(id);
    }

    @PostMapping("/{id}/packing")
    public ShipmentResponse pack(@PathVariable Long id) {
        return shipmentService.pack(id);
    }

    @PostMapping("/{id}/shipping")
    public ShipmentResponse ship(@PathVariable Long id) {
        return shipmentService.ship(id);
    }
}
