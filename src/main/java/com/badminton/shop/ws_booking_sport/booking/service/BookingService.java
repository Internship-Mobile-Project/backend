package com.badminton.shop.ws_booking_sport.booking.service;

import com.badminton.shop.ws_booking_sport.booking.repository.BookingRepository;
import com.badminton.shop.ws_booking_sport.booking.repository.SlotRepository;
import com.badminton.shop.ws_booking_sport.core.repository.CustomerRepository;
import com.badminton.shop.ws_booking_sport.core.repository.UserRepository;
import com.badminton.shop.ws_booking_sport.core.repository.AccountRepository;
import com.badminton.shop.ws_booking_sport.dto.request.BookingRequest;
import com.badminton.shop.ws_booking_sport.dto.response.BookingResponse;
import com.badminton.shop.ws_booking_sport.enums.BookingStatus;
import com.badminton.shop.ws_booking_sport.enums.PaymentStatus;
import com.badminton.shop.ws_booking_sport.handleException.ResourceUnavailableException;
import com.badminton.shop.ws_booking_sport.model.booking.Booking;
import com.badminton.shop.ws_booking_sport.model.booking.CashPayment;
import com.badminton.shop.ws_booking_sport.model.booking.OnlinePayment;
import com.badminton.shop.ws_booking_sport.model.booking.Payment;
import com.badminton.shop.ws_booking_sport.model.core.Account;
import com.badminton.shop.ws_booking_sport.model.core.Customer;
import com.badminton.shop.ws_booking_sport.model.core.User;
import com.badminton.shop.ws_booking_sport.model.venue.Field;
import com.badminton.shop.ws_booking_sport.model.venue.PriceRule;
import com.badminton.shop.ws_booking_sport.model.venue.Slot;
import com.badminton.shop.ws_booking_sport.venue.repository.FieldRepository;
import com.badminton.shop.ws_booking_sport.venue.repository.PriceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final FieldRepository fieldRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public Booking createBooking(BookingRequest req) {
        // keep backward-compatible: if frontend still sends customerId, use previous flow
        if (req.getCustomerId() != null) {
            return createBookingWithProvidedCustomer(req, req.getCustomerId());
        }
        throw new IllegalArgumentException("customerId is required in this endpoint. Use authenticated endpoint instead.");
    }

    // New API used by controller: create booking using authenticated user's id extracted from JWT (userId)
    @Transactional
    public Booking createBookingForUser(BookingRequest req, Integer userId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");

        // resolve Customer from userId
        Customer customer = customerRepository.findById(userId).orElse(null);
        if (customer == null) {
            // try find Account linked to this user id
            Account acc = accountRepository.findByUserId(userId).orElse(null);
            if (acc != null && acc.getUser() != null) {
                User u = acc.getUser();
                if (u instanceof Customer) {
                    customer = (Customer) u;
                } else {
                    customer = customerRepository.findById(u.getId()).orElse(null);
                }
            }
        }
        if (customer == null) {
            User u = userRepository.findById(userId).orElse(null);
            if (u != null && u instanceof Customer) {
                customer = (Customer) u;
            }
        }
        if (customer == null) {
            throw new IllegalArgumentException("Authenticated user is not a Customer or could not be resolved to a Customer");
        }

        return createBookingWithCustomer(req, customer);
    }

    // internal: booking flow when customer is already resolved
    private Booking createBookingWithCustomer(BookingRequest req, Customer customer) {
        if (req == null) throw new IllegalArgumentException("Request body is required");

        Integer fieldId = req.getFieldId();
        Integer venueId = req.getVenueId();
        LocalDate date = req.getDate();
        LocalTime start = req.getStartTime();
        LocalTime end = req.getEndTime();

        if (fieldId == null || date == null || start == null || end == null) {
            throw new IllegalArgumentException("fieldId, date, startTime and endTime are required");
        }
        if (!start.isBefore(end)) throw new IllegalArgumentException("startTime must be before endTime");

        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        if (field.getVenue() == null || !field.getVenue().getId().equals(venueId)) {
            throw new IllegalArgumentException("Field does not belong to the specified venue");
        }

        // check overlapping slots
        List<Slot> overlapping = slotRepository.findOverlappingSlots(fieldId, date, start, end);
        if (overlapping != null && !overlapping.isEmpty()) {
            throw new ResourceUnavailableException("Requested time range overlaps with existing bookings or slots");
        }

        // load price rules for the field
        List<PriceRule> rules = priceRuleRepository.findByFieldId(fieldId);
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Mon .. 7=Sun

        long totalRequestedMinutes = Duration.between(start, end).toMinutes();
        if (totalRequestedMinutes <= 0) throw new IllegalArgumentException("Invalid booking duration");

        // calculate total price by summing overlap between request interval and each price rule for the day
        double totalPrice = 0.0;
        long coveredMinutes = 0;

        for (PriceRule pr : rules) {
            if (pr.getDayOfWeek() != dayOfWeek) continue;
            LocalTime ruleStart = pr.getStartTime();
            LocalTime ruleEnd = pr.getEndTime();

            // compute overlap between [start,end) and [ruleStart,ruleEnd)
            LocalTime overlapStart = start.isAfter(ruleStart) ? start : ruleStart;
            LocalTime overlapEnd = end.isBefore(ruleEnd) ? end : ruleEnd;
            if (overlapStart.isBefore(overlapEnd)) {
                long minutes = Duration.between(overlapStart, overlapEnd).toMinutes();
                coveredMinutes += minutes;
                totalPrice += (minutes / 60.0) * pr.getPricePerHour();
            }
        }

        if (coveredMinutes < totalRequestedMinutes) {
            throw new IllegalArgumentException("Price rules do not cover the entire booking period");
        }

        // create slot entities per 30-minute chunks and compute per-slot price proportional to rules
        List<Slot> slotsToSave = new ArrayList<>();
        LocalTime cursor = start;
        while (cursor.isBefore(end)) {
            LocalTime slotEnd = cursor.plusMinutes(30);
            if (slotEnd.isAfter(end)) slotEnd = end;

            // compute price for this slot by checking overlaps with rules
            double slotPrice = 0.0;
            for (PriceRule pr : rules) {
                if (pr.getDayOfWeek() != dayOfWeek) continue;
                LocalTime overlapStart = cursor.isAfter(pr.getStartTime()) ? cursor : pr.getStartTime();
                LocalTime overlapEnd = slotEnd.isBefore(pr.getEndTime()) ? slotEnd : pr.getEndTime();
                if (overlapStart.isBefore(overlapEnd)) {
                    long minutes = Duration.between(overlapStart, overlapEnd).toMinutes();
                    slotPrice += (minutes / 60.0) * pr.getPricePerHour();
                }
            }

            Slot s = new Slot();
            s.setDate(date);
            s.setStartTime(cursor);
            s.setEndTime(slotEnd);
            s.setStatus(com.badminton.shop.ws_booking_sport.enums.SlotStatus.BOOKED);
            s.setFinalPrice(slotPrice);
            s.setField(field);
            slotsToSave.add(s);

            cursor = slotEnd;
        }

        List<Slot> savedSlots = slotRepository.saveAll(slotsToSave);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setField(field);
        booking.setSlot(savedSlots);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setBookedAt(LocalDateTime.now());
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        // handle payment creation according to request
        String pm = req.getPaymentMethod();
        if (pm != null) {
            if ("CASH".equalsIgnoreCase(pm)) {
                CashPayment cp = new CashPayment();
                cp.setAmount(totalPrice);
                cp.setCurrency("VND");
                cp.setStatus(PaymentStatus.PENDING);
                cp.setCreatedAt(LocalDateTime.now());
                cp.setBooking(booking);
                booking.setPayment(cp);
            } else if ("ONLINE".equalsIgnoreCase(pm)) {
                OnlinePayment op = new OnlinePayment();
                op.setAmount(totalPrice);
                op.setCurrency("VND");
                op.setStatus(PaymentStatus.PENDING);
                op.setCreatedAt(LocalDateTime.now());
                String provider = req.getPaymentProvider() != null ? req.getPaymentProvider().toUpperCase() : "VNPAY";
                op.setMethod(provider);
                // generate a simple redirect url for the demo (frontend will redirect user to this URL)
                String redirect = "https://pay.example.com/" + provider + "/pay?bookingId=" + "{bookingId}" + "&amount=" + (long) totalPrice;
                // placeholder: we don't have booking id until saved; store placeholder and replace after saving
                op.setRedirectUrl(redirect);
                op.setBooking(booking);
                booking.setPayment(op);
            }
        }

        Booking saved = bookingRepository.save(booking);

        // if online payment created we should replace placeholder bookingId in redirectUrl
        if (saved.getPayment() instanceof OnlinePayment) {
            OnlinePayment op = (OnlinePayment) saved.getPayment();
            String redirect = op.getRedirectUrl();
            if (redirect != null && redirect.contains("{bookingId}")) {
                redirect = redirect.replace("{bookingId}", saved.getId());
                op.setRedirectUrl(redirect);
                // save updated payment
                bookingRepository.save(saved);
            }
        }

        return saved;
    }

    // keep existing helper for backward compatible path
    private Booking createBookingWithProvidedCustomer(BookingRequest req, Integer providedId) {
        // reuse previous resolution logic: attempt to resolve providedId to Customer
        Integer originalProvided = providedId;
        Customer customer = customerRepository.findById(providedId).orElse(null);
        if (customer == null) {
            Account acc = accountRepository.findByUserId(providedId).orElse(null);
            if (acc != null && acc.getUser() != null) {
                User u = acc.getUser();
                if (u instanceof Customer) {
                    customer = (Customer) u;
                } else {
                    customer = customerRepository.findById(u.getId()).orElse(null);
                }
            }
        }
        if (customer == null) {
            User u = userRepository.findById(providedId).orElse(null);
            if (u != null && u instanceof Customer) customer = (Customer) u;
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found for provided id: " + originalProvided);
        }
        return createBookingWithCustomer(req, customer);
    }

    // Map Booking entity to DTO for API responses
    public BookingResponse toBookingResponse(Booking booking) {
        if (booking == null) return null;
        BookingResponse resp = new BookingResponse();
        resp.setId(booking.getId());
        if (booking.getCustomer() != null) {
            resp.setCustomerId(booking.getCustomer().getId());
            resp.setCustomerName(booking.getCustomer().getName());
        }
        if (booking.getField() != null) {
            resp.setFieldId(booking.getField().getId());
            resp.setFieldName(booking.getField().getName());
        }
        resp.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        resp.setTotalPrice(booking.getTotalPrice());
        resp.setBookedAt(booking.getBookedAt());
        resp.setCreatedAt(booking.getCreatedAt());
        resp.setUpdatedAt(booking.getUpdatedAt());

        List<BookingResponse.SlotInfo> slots = new ArrayList<>();
        if (booking.getSlot() != null) {
            slots = booking.getSlot().stream().map(s -> new BookingResponse.SlotInfo(
                    s.getDate(), s.getStartTime(), s.getEndTime(), s.getFinalPrice()
            )).collect(Collectors.toList());
        }
        resp.setSlots(slots);

        // payment info
        if (booking.getPayment() != null) {
            Payment p = booking.getPayment();
            BookingResponse.PaymentInfo pi = new BookingResponse.PaymentInfo();
            if (p instanceof OnlinePayment) {
                OnlinePayment op = (OnlinePayment) p;
                pi.setMethod(op.getMethod());
                pi.setRedirectUrl(op.getRedirectUrl());
                pi.setAmount(op.getAmount());
                pi.setCurrency(op.getCurrency());
            } else if (p instanceof CashPayment) {
                CashPayment cp = (CashPayment) p;
                pi.setMethod("CASH");
                pi.setAmount(cp.getAmount());
                pi.setCurrency(cp.getCurrency());
            } else {
                pi.setMethod(null);
            }
            pi.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
            resp.setPayment(pi);
        }

        return resp;
    }

    // Owner updates cash payment status for a booking
    @Transactional
    public Booking ownerUpdateCashPaymentStatus(String bookingId, Integer ownerId, PaymentStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getField() == null || booking.getField().getVenue() == null || booking.getField().getVenue().getOwner() == null) {
            throw new IllegalArgumentException("Booking does not have an associated venue/owner");
        }
        Integer venueOwnerId = booking.getField().getVenue().getOwner().getId();
        if (!venueOwnerId.equals(ownerId)) {
            throw new IllegalArgumentException("Only the owner of the venue can update this payment");
        }
        if (!(booking.getPayment() instanceof CashPayment)) {
            throw new IllegalArgumentException("Payment for this booking is not a cash payment");
        }
        CashPayment cp = (CashPayment) booking.getPayment();
        cp.setStatus(newStatus);
        if (newStatus == PaymentStatus.SUCCESS) {
            cp.setPaidAt(LocalDateTime.now());
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    // Customer initiates online payment (returns redirect url)
    @Transactional(readOnly = true)
    public String initiateOnlinePayment(String bookingId, Integer customerId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Only the customer who created the booking can initiate its online payment");
        }
        if (!(booking.getPayment() instanceof OnlinePayment)) {
            throw new IllegalArgumentException("Payment for this booking is not an online payment");
        }
        OnlinePayment op = (OnlinePayment) booking.getPayment();
        return op.getRedirectUrl();
    }
}
