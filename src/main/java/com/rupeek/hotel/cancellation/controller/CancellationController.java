package com.rupeek.hotel.cancellation.controller;

import com.rupeek.hotel.cancellation.dto.CancellationResult;
import com.rupeek.hotel.cancellation.service.CancellationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CancellationController {

    private final CancellationService cancellationService;

    public CancellationController(CancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping("/api/bookings/{bookingId}/cancel")
    public CancellationResult cancel(@PathVariable String bookingId) {
        return cancellationService.cancel(bookingId);
    }
}
