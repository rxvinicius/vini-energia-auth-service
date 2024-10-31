package com.vini_energia.auth_service.controllers;

import com.vini_energia.auth_service.models.User;
import com.vini_energia.auth_service.repositories.UserRepository;
import com.vini_energia.auth_service.services.JwtService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
    }

    @Test
    void testRegisterUserSuccess() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<String> response = authController.register(testUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUserConflict() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(testUser);

        ResponseEntity<String> response = authController.register(testUser);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already in use", response.getBody());
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

        ResponseEntity<String> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("dummyToken", response.getBody());
    }

    @Test
    void testLoginFailure() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(null);

        ResponseEntity<String> response = authController.login(testUser);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password", response.getBody());
    }

}
