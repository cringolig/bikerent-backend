package com.company.bikerent.integration;

import com.company.bikerent.auth.dto.TokenResponse;
import com.company.bikerent.auth.dto.RegisterRequest;
import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.domain.BicycleStatus;
import com.company.bikerent.bicycle.domain.BicycleType;
import com.company.bikerent.bicycle.repository.BicycleRepository;
import com.company.bikerent.billing.dto.CreatePaymentRequest;
import com.company.bikerent.geo.domain.Coordinates;
import com.company.bikerent.rental.dto.CompleteRentalRequest;
import com.company.bikerent.rental.dto.CreateRentalRequest;
import com.company.bikerent.station.domain.Station;
import com.company.bikerent.station.repository.StationRepository;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RentalFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private BicycleRepository bicycleRepository;

    private String authToken;
    private User testUser;
    private Station startStation;
    private Station endStation;
    private Bicycle testBicycle;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("rentaluser", "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class);
        authToken = tokenResponse.accessToken();
        testUser = userRepository.findByUsername("rentaluser").orElseThrow();

        startStation = new Station();
        startStation.setName("Start Station");
        startStation.setCoordinates(Coordinates.builder().latitude(55.0f).longitude(37.0f).build());
        startStation.setAvailableBicycles(1L);
        startStation = stationRepository.save(startStation);

        endStation = new Station();
        endStation.setName("End Station");
        endStation.setCoordinates(Coordinates.builder().latitude(55.1f).longitude(37.1f).build());
        endStation.setAvailableBicycles(0L);
        endStation = stationRepository.save(endStation);

        testBicycle = new Bicycle();
        testBicycle.setModel("Test Bike");
        testBicycle.setType(BicycleType.MOUNTAIN);
        testBicycle.setStatus(BicycleStatus.AVAILABLE);
        testBicycle.setStation(startStation);
        testBicycle.setMileage(0L);
        testBicycle = bicycleRepository.save(testBicycle);
    }

    @Test
    @DisplayName("Complete rental flow: add balance, rent, return")
    void shouldCompleteFullRentalFlow() throws Exception {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(1000L);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1000));

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isEqualTo(1000L);

        CreateRentalRequest rentalRequest = new CreateRentalRequest(
                testUser.getId(), testBicycle.getId(), startStation.getId());
        
        MvcResult rentalResult = mockMvc.perform(post("/api/v1/rentals")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rentalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        Bicycle rentedBicycle = bicycleRepository.findById(testBicycle.getId()).orElseThrow();
        assertThat(rentedBicycle.getStatus()).isEqualTo(BicycleStatus.RENTED);

        Long rentalId = objectMapper.readTree(rentalResult.getResponse().getContentAsString())
                .get("id").asLong();

        CompleteRentalRequest completeRequest = new CompleteRentalRequest(endStation.getId());
        mockMvc.perform(put("/api/v1/rentals/" + rentalId + "/complete")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));

        Bicycle returnedBicycle = bicycleRepository.findById(testBicycle.getId()).orElseThrow();
        assertThat(returnedBicycle.getStatus()).isEqualTo(BicycleStatus.AVAILABLE);
        assertThat(returnedBicycle.getStation().getId()).isEqualTo(endStation.getId());
    }

    @Test
    @DisplayName("Should reject rental when user has no balance")
    void shouldRejectRentalWithNoBalance() throws Exception {
        CreateRentalRequest rentalRequest = new CreateRentalRequest(
                testUser.getId(), testBicycle.getId(), startStation.getId());

        mockMvc.perform(post("/api/v1/rentals")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rentalRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should reject rental when bicycle is already rented")
    void shouldRejectRentalOfAlreadyRentedBicycle() throws Exception {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(1000L);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated());

        testBicycle.setStatus(BicycleStatus.RENTED);
        bicycleRepository.save(testBicycle);

        CreateRentalRequest rentalRequest = new CreateRentalRequest(
                testUser.getId(), testBicycle.getId(), startStation.getId());

        mockMvc.perform(post("/api/v1/rentals")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rentalRequest)))
                .andExpect(status().isConflict());
    }
}
