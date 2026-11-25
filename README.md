# Maid Booking Platform (take-home)

Backend for a home-services marketplace: customers discover maids and book them (instant, scheduled or
recurring), pay per visit, and cancel a single visit or a whole series with a pluggable refund policy.

Java 17 · Spring Boot 3.3 · in-memory persistence behind repository interfaces.

## Build, test, run

```bash
mvn test                 # unit tests
mvn spring-boot:run      # starts on http://localhost:8080
```

Swagger UI (bonus): http://localhost:8080/swagger-ui.html. Three demo maids are seeded on start-up
(`maid.seed-demo-data=false` disables this).

### Try it

```bash
# 1. find cleaners in Indiranagar free next Wednesday 10:00-12:00 (IST)
curl "localhost:8080/maids/search?locality=Indiranagar&service=CLEANING&from=2026-12-02T04:30:00Z&to=2026-12-02T06:30:00Z&minRating=4"

# 2. onboard a maid
curl -X POST localhost:8080/maids -H 'Content-Type: application/json' -d '{
  "name":"Lata","locality":"HSR","skills":["COOKING"],"hourlyRate":280,"gender":"FEMALE",
  "availability":[{"day":"MONDAY","from":"09:00","to":"17:00"}]}'

# 3. scheduled booking (maid auto-selected when maidId is omitted)
curl -X POST localhost:8080/bookings -H 'Content-Type: application/json' -d '{
  "customerId":"c1","locality":"Indiranagar","serviceType":"CLEANING","bookingType":"SCHEDULED",
  "start":"2026-12-02T04:30:00Z","durationMinutes":120}'

# 4. recurring: every weekday at the start's time of day until 2026-12-18
curl -X POST localhost:8080/bookings -H 'Content-Type: application/json' -d '{
  "customerId":"c1","locality":"Indiranagar","serviceType":"CLEANING","bookingType":"RECURRING",
  "start":"2026-12-02T03:30:00Z","durationMinutes":60,
  "recurrence":{"days":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"],"until":"2026-12-18"}}'

# 5. pay one occurrence (card 16 digits / UPI id / wallet id; use a fresh idempotencyKey per attempt)
curl -X POST localhost:8080/bookings/{bookingId}/occurrences/{occurrenceId}/payments \
  -H 'Content-Type: application/json' \
  -d '{"method":"CARD","instrument":"4111111111111111","idempotencyKey":"pay-1"}'

# 6. cancel one occurrence / the whole series
curl -X DELETE localhost:8080/bookings/{bookingId}/occurrences/{occurrenceId}
curl -X DELETE localhost:8080/bookings/{bookingId}
```

Simulated gateway: any instrument containing "fail" or ending in `0000` is declined.
Status codes: 400 invalid input, 402 payment declined, 404 not found, 409 slot taken / invalid state transition.

## Design

```
api/          controllers + DTOs + exception -> HTTP mapping      (Spring lives here and in config/)
service/      BookingService, PaymentService, CancellationService, MaidService   (plain Java)
domain/       Maid, Booking, Occurrence, Payment, Money, TimeRange ...           (plain Java)
booking/      BookingStrategy + Instant / Scheduled / Recurring, RecurrenceRule
search/       MaidFilter + one class per criterion, MaidSearchService
payment/      PaymentProcessor + Card / UPI / Wallet (simulated), registry
cancellation/ CancellationPolicy + time-based refund tiers
pricing/      PricingStrategy + hourly rate
repository/   interfaces (+ memory/ implementations)
config/       AppConfig: the one place that picks implementations
```

Everything outside `api/` and `config/` has no Spring dependency; collaborators arrive through constructors.

**Booking = series of Occurrences.** A one-off booking has one occurrence; a recurring booking has many. Payment,
cancellation and slot release all work per occurrence, so single-occurrence cancellation and per-occurrence
payment need no special cases. Booking status is derived from its occurrences and can never disagree with them.

**Occurrence state machine:** `PENDING_PAYMENT -> CONFIRMED -> COMPLETED`; `CANCELLED` is reachable from the first two.
Anything else throws `InvalidStateTransitionException`.

**Extension points** (each is one new class plus, for Spring, one `@Bean`; nothing existing changes):

| To add... | Implement |
|---|---|
| a booking type (e.g. weekly package) | `BookingStrategy` (+ a `BookingType` value) |
| a search filter | `MaidFilter` |
| a payment method | `PaymentProcessor` (+ a `PaymentMethodType` value) |
| a refund rule | `CancellationPolicy` |
| dynamic pricing | `PricingStrategy` |
| a database | the four `*Repository` interfaces |

**Double-booking and concurrency.** All shared inventory lives in `ScheduleRepository`. `reserve(maidId, slots)`
is atomic and all-or-nothing under a per-maid lock, so two simultaneous requests for the same slot cannot both
succeed, and a recurring series either reserves every date or none. Automatic maid selection does not trust the
search result: it tries candidates best-first and lets `reserve` decide, skipping anyone who was just taken.
`Occurrence` transitions are synchronized, and payments serialise per occurrence. With a real database the same
contract maps to a unique/exclusion constraint or optimistic locking.

**Payments.** Taken per occurrence, so recurring visits are paid one at a time. Idempotency keys are required:
a retry returns the original payment without charging again; reusing a key for another occurrence is rejected.

**Cancellation.** `OCCURRENCE` cancels one visit; `SERIES` cancels every upcoming visit and leaves past or finished
ones alone. Default policy: 24h+ notice = 100% refund, 2-24h = 50%, under 2h = 0%. Only paid visits are refunded.
The slot is released in every case, so the maid is discoverable and bookable again immediately.

## Assumptions

- A single platform time zone (`maid.zone`, default Asia/Kolkata) defines working hours and recurring slots.
- Working windows are weekly and do not span midnight; a slot must fit entirely inside one window.
- Duration is 30 minutes to 8 hours. Recurring series are capped at 84 days. The first slot anchors the time of
  day, and occurrences start from its date.
- Instant booking means "start now" for the requested duration.
- Customers are identified by a plain `customerId`; there is no authentication (out of scope).
- Maid rating is supplied at onboarding (default 0), standing in for a future review system.
- Searching by time uses the requested slot; automatic selection for a recurring series checks the whole series.

## With more time

- Persistence via JPA with optimistic locking / an exclusion constraint on `(maid_id, time_range)`.
- A saga/outbox around cancel -> refund -> release so a failed refund can be retried without losing the release.
- Scheduled job to move past occurrences to `COMPLETED`, and automatic payment for each upcoming recurring visit.
- Real gateway adapters, a webhook for async payment confirmation, richer pricing (peak hours, discounts).
- Pagination and sorting options on search; `@WebMvcTest` slices and an end-to-end `@SpringBootTest`.
- Bookings that span midnight and per-maid time zones.
