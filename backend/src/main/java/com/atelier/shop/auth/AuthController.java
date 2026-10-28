package com.atelier.shop.auth;

import com.atelier.shop.config.JwtService;
import com.atelier.shop.domain.Role;
import com.atelier.shop.domain.User;
import com.atelier.shop.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = users.save(new User(request.name().trim(), request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()), Role.USER));
        return response(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));
        return response(user);
    }

    @GetMapping("/me")
    public UserSummary currentUser(@AuthenticationPrincipal User user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.createToken(user), new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }

    public record RegisterRequest(@NotBlank @Size(max = 100) String name, @NotBlank @Email @Size(max = 190) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserSummary(Long id, String name, String email, String role) {}
    public record AuthResponse(String token, UserSummary user) {}
}
