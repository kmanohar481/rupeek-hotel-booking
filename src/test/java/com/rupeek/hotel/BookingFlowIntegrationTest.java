package com.rupeek.hotel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the whole flow through the REST API: onboard -> search -> book -> pay -> cancel -> search again.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIntegrationTest {

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void guestCanSearchBookPayAndCancel() throws Exception {
        JsonNode owner = onboardHotelInLocality("Jubilee Hills");
        String propertyId = owner.at("/properties/0/id").asText();
        String roomTypeId = owner.at("/properties/0/roomTypes/0/id").asText();

        searchJubileeHills()
                .andExpect(jsonPath("$[0].propertyId").value(propertyId))
                .andExpect(jsonPath("$[0].availableRooms[0].totalPrice").value(7000.00));

        String bookingJson = createBooking(propertyId, roomTypeId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(7000.00))
                .andReturn().getResponse().getContentAsString();
        String bookingId = objectMapper.readTree(bookingJson).get("bookingId").asText();

        // the only room is held, so the property drops out of search and a second booking fails
        searchJubileeHills().andExpect(jsonPath("$.length()").value(0));
        createBooking(propertyId, roomTypeId).andExpect(status().isConflict());

        pay(bookingId, "key-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
        pay(bookingId, "key-2").andExpect(status().isConflict());

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(7000.00));

        mockMvc.perform(get("/api/bookings/{id}", bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        searchJubileeHills().andExpect(jsonPath("$[0].propertyId").value(propertyId));
    }

    @Test
    void returnsClearErrorsForBadRequests() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"No Properties\", \"email\": \"x@example.com\", \"properties\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("properties")));

        mockMvc.perform(post("/api/bookings/{id}/payments", "any").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\": \"CARD\", \"details\": {\"cardNumber\": \"4111111111111111\"}}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/properties/search")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Either city or locality is required"));
    }

    private JsonNode onboardHotelInLocality(String locality) throws Exception {
        String body = """
                {
                  "name": "Deccan Hospitality",
                  "email": "owner@example.com",
                  "properties": [{
                    "name": "Deccan Grand",
                    "type": "HOTEL",
                    "city": "Hyderabad",
                    "locality": "%s",
                    "addressLine": "Road No. 36",
                    "starRating": 4,
                    "amenities": ["WIFI", "POOL"],
                    "cancellationPolicy": "FLEXIBLE",
                    "roomTypes": [{"name": "Deluxe", "maxGuests": 2, "totalRooms": 1, "pricePerNight": 3500}]
                  }]
                }
                """.formatted(locality);
        String response = mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private ResultActions searchJubileeHills() throws Exception {
        return mockMvc.perform(get("/api/properties/search")
                        .param("locality", "Jubilee Hills")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString())
                        .param("guests", "2")
                        .param("amenities", "WIFI", "POOL"))
                .andExpect(status().isOk());
    }

    private ResultActions createBooking(String propertyId, String roomTypeId) throws Exception {
        String body = """
                {"propertyId": "%s", "roomTypeId": "%s", "guestName": "Guest One",
                 "checkIn": "%s", "checkOut": "%s", "guests": 2}
                """.formatted(propertyId, roomTypeId, CHECK_IN, CHECK_OUT);
        return mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions pay(String bookingId, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/bookings/{id}/payments", bookingId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\": \"UPI\", \"details\": {\"vpa\": \"guest@okbank\"}}"));
    }
}
