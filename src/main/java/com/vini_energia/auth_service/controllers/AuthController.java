package com.vini_energia.auth_service.controllers;

import com.vini_energia.auth_service.dtos.LoginResponse;
import com.vini_energia.auth_service.dtos.RegisterResponse;
import com.vini_energia.auth_service.dtos.UserDetails;
import com.vini_energia.auth_service.models.User;
import com.vini_energia.auth_service.repositories.UserRepository;
import com.vini_energia.auth_service.services.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody User user) {
        // TODO: create a RegisterRequest class to validate the params or use @NotEmpty annotations in User model
        if (
                user.getEmail() == null || user.getEmail().isEmpty() ||
                user.getPassword() == null || user.getPassword().isEmpty() ||
                user.getName() == null || user.getName().isEmpty()
        ) {
            return ResponseEntity.badRequest().body(new RegisterResponse(false, "Email, password as name must not be empty", null));
        }

        try {
            User foundUser = userRepository.findByEmail(user.getEmail());
            if (foundUser != null) {
                return ResponseEntity.status((HttpStatus.CONFLICT))
                        .body(new RegisterResponse(false, "Email already in use", null));
            }

            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            userRepository.save(user);

            String token = jwtService.generateToken(user.getEmail());
            UserDetails userDetails = new UserDetails(user.getId().toHexString(), user.getEmail(), user.getName(), token);
            RegisterResponse response = new RegisterResponse(true, "User registered successfully", userDetails);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RegisterResponse(false, "Error registering user: " + e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody User loginRequest) {
        // TODO: create a LoginRequest class to validate the params
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new LoginResponse(false, "Email and password must not be empty", null));
        }

        Optional<User> user = Optional.ofNullable(userRepository.findByEmail(loginRequest.getEmail()));

        if (user.isPresent()) {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            if (passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
                String token = jwtService.generateToken(user.get().getEmail());
                LoginResponse response = new LoginResponse(
                        true,
                        "Login successful",
                        new UserDetails(
                                user.get().getId().toHexString(),
                                user.get().getEmail(),
                                user.get().getName(),
                                token
                        )
                );

                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(false, "Invalid email or password", null));
    }

}
