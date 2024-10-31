package com.vini_energia.auth_service.controllers;

import com.vini_energia.auth_service.dtos.LoginResponse;
import com.vini_energia.auth_service.dtos.RegisterResponse;
import com.vini_energia.auth_service.models.User;
import com.vini_energia.auth_service.repositories.UserRepository;
import com.vini_energia.auth_service.services.JwtService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(new ObjectId());
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("password123");
    }

    @Test
    void testRegisterUserSuccess() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<RegisterResponse> response = authController.register(testUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        RegisterResponse body = response.getBody();

        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User registered successfully", body.getMessage());
        assertNotNull(body.getUser());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUserConflict() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(testUser);

        ResponseEntity<RegisterResponse> response = authController.register(testUser);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        RegisterResponse body = response.getBody();

        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Email already in use", body.getMessage());
        assertNull(body.getUser());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        String textPassword = testUser.getPassword();
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        testUser.setPassword(encoder.encode(textPassword));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(jwtService.generateToken(testUser.getEmail())).thenReturn("dummyToken");

        User loginRequest = new User();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword(textPassword);

        ResponseEntity<LoginResponse> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getUser());
        assertEquals("dummyToken", response.getBody().getUser().getToken());
        assertEquals(testUser.getEmail(), response.getBody().getUser().getEmail());
        assertEquals(testUser.getName(), response.getBody().getUser().getName());
        assertEquals(testUser.getId().toHexString(), response.getBody().getUser().getId());
    }

    @Test
    void testLoginFailure() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(null);

        User loginRequest = new User();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword("wrongPassword");

        ResponseEntity<LoginResponse> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getUser());
        assertEquals("Invalid email or password", response.getBody().getMessage());
        assertNull(response.getBody().getUser());
    }

}
