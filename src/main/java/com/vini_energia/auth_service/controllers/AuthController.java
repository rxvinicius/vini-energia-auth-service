package com.vini_energia.auth_service.controllers;

import com.vini_energia.auth_service.dtos.LoginResponse;
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
    public ResponseEntity<String> register(@Valid @RequestBody User user) {
        try {
            User foundUser = userRepository.findByEmail(user.getEmail());
            if (foundUser != null) {
                return ResponseEntity.status((HttpStatus.CONFLICT)).body(("Email already in use"));
            }

            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error registering user: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody User loginRequest) {
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
