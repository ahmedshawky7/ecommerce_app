package com.example.ecommerce.integration;

import com.example.ecommerce.dto.AuthResponse;
import com.example.ecommerce.dto.LoginRequest;
import com.example.ecommerce.dto.RefreshTokenRequest;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.NotificationProducer;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SecurityAndAuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @MockitoBean
    private NotificationProducer notificationProducer;

    private MockMvc mockMvc;
    private final Gson gson = new Gson();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        databaseCleaner.cleanAll();
    }

    @Test
    void customerShouldBeForbiddenFromAdminAndProductWriteEndpoints() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("cust1", "cust1@test.com", "secret123");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(registerReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = gson.fromJson(regResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + auth.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hack\",\"price\":10,\"stockQuantity\":5,\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenRotationAndReuseDetectionShouldWork() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new RegisterRequest("rotuser", "rot@test.com", "secret123"))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new LoginRequest("rot@test.com", "secret123"))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse firstAuth = gson.fromJson(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // First refresh succeeds and rotates token
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new RefreshTokenRequest(firstAuth.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse secondAuth = gson.fromJson(refreshResult.getResponse().getContentAsString(), AuthResponse.class);

        // Reusing old refresh token fails and revokes all user sessions
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new RefreshTokenRequest(firstAuth.refreshToken()))))
                .andExpect(status().isBadRequest());

        // Even the second refresh token is now revoked due to reuse detection
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new RefreshTokenRequest(secondAuth.refreshToken()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disabledUserShouldImmediatelyGetUnauthorizedWithExistingJwt() throws Exception {
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new RegisterRequest("banned", "banned@test.com", "secret123"))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = gson.fromJson(regResult.getResponse().getContentAsString(), AuthResponse.class);

        User user = userRepository.findByEmail("banned@test.com").orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isUnauthorized());
    }
}
