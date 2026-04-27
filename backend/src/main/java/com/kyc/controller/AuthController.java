package com.kyc.controller;

import com.kyc.dto.AuthDto;
import com.kyc.entity.User;
import com.kyc.repository.UserRepository;
import com.kyc.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user registration and login.
 * These endpoints are public (no JWT required).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthDto.SignupRequest request) {
        // Check if username already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(errorResponse("Username already taken: " + request.getUsername()));
        }

        // Default role is MERCHANT; only allow MERCHANT signups publicly
        User.Role role = User.Role.MERCHANT;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse("Invalid role. Use MERCHANT or REVIEWER."));
        }

        // Create and save user with hashed password
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                role
        );
        userRepository.save(user);

        // Return a token immediately so user can start using the app
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthDto.AuthResponse(token, user.getUsername(), role.name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDto.LoginRequest request) {
        try {
            // Spring Security handles password verification here
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthDto.AuthResponse(token, user.getUsername(), user.getRole().name()));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(errorResponse("Invalid username or password."));
        }
    }

    private java.util.Map<String, String> errorResponse(String message) {
        return java.util.Map.of("error", message);
    }
}
