package com.example.maid.api;

import com.example.maid.api.dto.BookingResponse;
import com.example.maid.api.dto.CancelResponse;
import com.example.maid.api.dto.CreateBookingRequest;
import com.example.maid.api.dto.PayRequest;
import com.example.maid.api.dto.PaymentResponse;
import com.example.maid.cancellation.CancellationScope;
import com.example.maid.payment.PaymentRequest;
import com.example.maid.service.BookingService;
import com.example.maid.service.CancellationService;
import com.example.maid.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    public BookingController(BookingService bookingService, PaymentService paymentService,
                             CancellationService cancellationService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.cancellationService = cancellationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.book(request.toDomain()));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(bookingService.get(bookingId));
    }

    @PostMapping("/{bookingId}/occurrences/{occurrenceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable String bookingId, @PathVariable String occurrenceId,
                               @Valid @RequestBody PayRequest request) {
        return PaymentResponse.from(paymentService.pay(new PaymentRequest(
                bookingId, occurrenceId, request.method(), request.instrument(), request.idempotencyKey())));
    }

    /** Cancels every upcoming occurrence of the booking. */
    @DeleteMapping("/{bookingId}")
    public CancelResponse cancelSeries(@PathVariable String bookingId) {
        return CancelResponse.from(cancellationService.cancel(bookingId, null, CancellationScope.SERIES));
    }

    @DeleteMapping("/{bookingId}/occurrences/{occurrenceId}")
    public CancelResponse cancelOccurrence(@PathVariable String bookingId, @PathVariable String occurrenceId) {
        return CancelResponse.from(
                cancellationService.cancel(bookingId, occurrenceId, CancellationScope.OCCURRENCE));
    }
}
