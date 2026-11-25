# Hotel Booking Service

Backend for a hotel booking platform: owners onboard properties, guests search, book, pay and cancel.
Java 17 + Spring Boot 3, in-memory storage behind repository interfaces.

## Build & run

```bash
mvn clean test                 # 80 tests: unit, concurrency and one end-to-end API test
mvn spring-boot:run            # starts on http://localhost:8080
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Sample requests: [`requests.http`](requests.http) (VS Code "REST Client" extension), or the curl flow below.

Requires JDK 17+ and Maven 3.9+.

## API

| Method | Path | What it does |
|---|---|---|
| POST | `/api/owners` | Onboard an owner with 1..N properties |
| POST | `/api/owners/{ownerId}/properties` | Add a property to an existing owner |
| GET | `/api/owners/{ownerId}/properties` | Owner with all their properties |
| GET | `/api/properties/{propertyId}` | Property details |
| GET | `/api/properties/search` | Search available properties (params below) |
| POST | `/api/bookings` | Book a room type → `PENDING_PAYMENT`, room is held |
| GET | `/api/bookings/{bookingId}` | Booking details |
| POST | `/api/bookings/{bookingId}/payments` | Pay (header `Idempotency-Key` required) |
| POST | `/api/bookings/{bookingId}/cancel` | Cancel, refund per policy, release room |

Search params: `city` and/or `locality` (at least one), `checkIn` and `checkOut` (ISO dates, required),
`guests` (default 1), and optional `minPrice`, `maxPrice`, `amenities` (e.g. `WIFI,POOL`), `minRating`.

Errors come back as `{status, error, message}` with 400 (bad input), 404 (unknown id),
409 (room not available / invalid state change) or 502 (gateway refund failed).

### Quick flow with curl

```bash
# 1. onboard a standalone hotel (an owner with one property)
curl -s -X POST localhost:8080/api/owners -H 'Content-Type: application/json' -d '{
  "name": "Sea Breeze Hotels", "email": "owner@example.com",
  "properties": [{
    "name": "Sea Breeze", "type": "HOTEL", "city": "Goa", "locality": "Calangute",
    "starRating": 4, "amenities": ["WIFI", "POOL"], "cancellationPolicy": "FLEXIBLE",
    "roomTypes": [{"name": "Deluxe", "maxGuests": 2, "totalRooms": 1, "pricePerNight": 2000}]
  }]}'

# 2. search
curl -s 'localhost:8080/api/properties/search?city=Goa&checkIn=2027-01-10&checkOut=2027-01-12&guests=2'

# 3. book (use the ids from step 1)
curl -s -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{
  "propertyId": "<propertyId>", "roomTypeId": "<roomTypeId>", "guestName": "Guest One",
  "checkIn": "2027-01-10", "checkOut": "2027-01-12", "guests": 2}'

# 4. pay - CARD {"cardNumber"}, UPI {"vpa"} or WALLET {"walletId"}
curl -s -X POST localhost:8080/api/bookings/<bookingId>/payments -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pay-1' -d '{"method": "UPI", "details": {"vpa": "guest@okbank"}}'

# 5. cancel
curl -s -X POST localhost:8080/api/bookings/<bookingId>/cancel
```

The mock gateway declines card `4000000000000002`, UPI `fail@upi` and wallet `fail-wallet`,
so the failure path can be tried from the API too.

## Project structure

One folder per feature, with layer subfolders inside (only the ones a feature needs):

```
com.rupeek.hotel
├── property        owners + properties
│   ├── controller    OwnerController, PropertyController
│   ├── service       PropertyService
│   ├── repository    OwnerRepository, PropertyRepository (+ in-memory impls)
│   ├── model         Owner, Property, RoomType, Location
│   ├── dto           NewOwner, NewProperty, NewRoomType, OwnerPortfolio
│   └── constants     Amenity, PropertyType
├── search
│   ├── controller    SearchController
│   ├── service       SearchService
│   │   └── filter      SearchFilter + Location / GuestCapacity / PriceRange / Amenities / StarRating
│   └── dto           SearchCriteria, PropertySearchResult, AvailableRoom
├── booking
│   ├── controller    BookingController
│   ├── service       BookingService
│   ├── repository    BookingRepository (+ in-memory impl)
│   ├── model         Booking
│   ├── dto           NewBooking, BookingResponse
│   ├── constants     BookingStatus (the lifecycle)
│   └── scheduler     BookingExpiryJob
├── payment
│   ├── controller    PaymentController
│   ├── service       PaymentService
│   │   └── processor   PaymentProcessor + Card / Upi / Wallet
│   ├── repository    PaymentRepository (+ in-memory impl)
│   ├── gateway       PaymentGateway + MockPaymentGateway
│   ├── model         Payment
│   ├── dto           PaymentRequest, PaymentResponse, PaymentResult
│   └── constants     PaymentMethod, PaymentStatus
├── cancellation
│   ├── controller    CancellationController
│   ├── service       CancellationService
│   │   └── policy      CancellationPolicy + Flexible / NonRefundable
│   ├── dto           CancellationResult
│   └── constants     CancellationPolicyType
├── inventory
│   └── repository    InventoryRepository (+ in-memory impl with per-room-type locking)
├── pricing
│   └── service       PricingStrategy + Standard / Weekend
└── common          shared across features
    ├── model         Money, DateRange
    ├── exception     domain exceptions + GlobalExceptionHandler
    ├── constants     AppConstants
    ├── config        Clock bean
    └── utils         KeyedLock, InMemoryRepository base class
```

Model classes don't depend on Spring. Services depend on repository/gateway **interfaces**, never on the
in-memory classes. Tests mirror the same packages; `support/TestFixture` wires the real services with
in-memory repositories and a controllable clock.

## Key design decisions

**Owners and properties.** An `Owner` is an account; every `Property` carries its `ownerId`.
A standalone hotel is an owner with one property, a chain is an owner with many. Both go through the same
`POST /api/owners` call with a list of properties, so there is no separate code path for the single case.
All properties in the request are validated before anything is saved.

**Inventory per room type per night.** A `RoomType` has `totalRooms`. The inventory repository keeps a
booked count per (room type, night). A stay is available if every night has `booked < totalRooms`.
Check-out day is not a night, so back-to-back stays share a room.

**Booking lifecycle.**

```
PENDING_PAYMENT --pay ok------> CONFIRMED --cancel--> CANCELLED (refund per policy)
       |--------pay declined--> PAYMENT_FAILED   (room released)
       |--------not paid 15m--> EXPIRED          (room released)
       '--------cancel--------> CANCELLED        (room released, nothing to refund)
```

Allowed transitions live in `BookingStatus`; anything else throws `InvalidStateTransitionException` (409).
The room is held when the booking is created, not at payment time - that is what stops two guests paying for
the same room. `BookingExpiryJob` runs every minute and releases rooms of bookings not paid within
`booking.hold-duration`.
The price is fixed on the booking when it is created and payment always charges that amount.

**Extension points** (each is an interface + Spring beans, picked up automatically):

| To add... | Do this | Nothing else changes |
|---|---|---|
| A search filter | a `SearchFilter` bean + a field in `SearchCriteria` + a query param in `SearchController` | `SearchService` |
| A payment method | add a `PaymentMethod` value + a `PaymentProcessor` bean | `PaymentService` |
| A cancellation policy | add a `CancellationPolicyType` value + a `CancellationPolicy` bean | `CancellationService` |
| A pricing rule | a `PricingStrategy` bean with its own `@ConditionalOnProperty(pricing.strategy=...)` value | booking / search |
| A property kind | a `PropertyType` value | - |
| Real storage / gateway | implement the repository / `PaymentGateway` interface and remove the in-memory / mock bean | services |

**Search filters work on (property, room type) pairs**, not on the property as a whole. Otherwise a hotel
whose cheap room is sold out and whose expensive room is free would wrongly match a low price range.
Each result lists only the room types that pass every filter and are free, with the total price for the stay
from the same `PricingStrategy` used when booking, so the price shown is the price charged.

**Payments.** A declined payment is a normal result (200, `paymentStatus: FAILED`, booking
`PAYMENT_FAILED`), not an exception. A retry with the same `Idempotency-Key` returns the original payment
without calling the gateway again. Keys are scoped to the booking.

**Cancellation.** Each property chooses a policy. `FLEXIBLE`: 100% refund 7+ days before check-in,
50% for 2–6 days, 0% after that. `NON_REFUNDABLE`: 0%. Cancelling releases the room immediately and,
for a paid booking with a refund above zero, calls `gateway.refund`.

## Concurrency

| Race | How it's handled |
|---|---|
| Two guests booking the last room | `tryReserve` checks and reserves **all nights in one step** under a per-room-type lock |
| Stays that overlap only partly (10–12 and 11–13) | same: either every night is reserved or none is |
| Pay and cancel arriving together | every status change runs under a **per-booking lock** (`BookingService.withLockedBooking`) |
| Same payment retried in parallel | idempotency lookup happens inside the booking lock, so the second request sees the first |
| Two payments with different keys | the second finds the booking already `CONFIRMED` and fails **before** charging |
| Cancel twice / cancel after payment failure | blocked by the state machine, so the room is released exactly once |
| Expiry job vs. guest paying | job re-checks the booking under the same lock before expiring |

Locks are always taken in the same order (booking, then room type), so they cannot deadlock. Bookings for
different room types never wait on each other. Search only checks availability, it doesn't reserve
anything, so a room it shows can be taken before the guest books; the booking step checks and reserves in
one step, which is what actually guarantees no double booking.

`ConcurrencyTest` starts 10–20 threads at the same moment for each case (pay vs. cancel is a two-way race
repeated 50 times). As a check, the tests were run with the locks
removed and 4 of them failed (oversold rooms, double charges), so they do catch the races.

## Assumptions

- One booking = one room of one room type; `guests` must fit that room's `maxGuests`.
- Inventory is per room type (a count), not per individual room number.
- Single currency (INR). Prices are per room per night; the price filter uses the base nightly price.
- Stays are 1–30 nights; check-in can't be in the past. Cancellation is allowed only before the check-in date.
- Unpaid bookings hold the room for 15 minutes (`booking.hold-duration`), released within a minute after
  that by the expiry job.
- No authentication (out of scope), so guest identity is just a name on the booking.
- Single application instance; all state is in memory and lost on restart.

## Trade-offs

- **The gateway is called while the booking lock is held.** Simple and safe here because it only blocks the
  same booking. With a slow real gateway I'd add a `PAYMENT_IN_PROGRESS` state, call the gateway outside the
  lock and finish from the gateway's callback.
- **The room is released before the refund call.** If the refund fails the guest gets a 502 but the booking is
  already cancelled. In production refunds would go through a retry queue.
- **Search scans all properties.** Fine in memory; with a database it would query by city/locality with indexes.
- **Per-key locks are never cleaned up.** Fine for this exercise; a real deployment would use DB locking.

## With more time

- Swap in a database: conditional update (`UPDATE ... SET booked = booked + 1 WHERE booked < total`) or
  optimistic locking for inventory, unique constraint on (booking, idempotency key). That would also make it
  safe to run more than one instance.
- Booking several rooms in one booking, and guest accounts.
- Pricing per date / season set by owners, taxes, multiple currencies.
- Async refunds with retries; gateway webhooks.
- Index search by location, paginate results, sort by price / rating.
- Updating property details and inventory after onboarding.
