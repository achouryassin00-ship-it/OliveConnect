package com.oliveconnect.service;

import com.oliveconnect.dto.AuthResponse;
import com.oliveconnect.dto.LoginRequest;
import com.oliveconnect.dto.RegisterRequest;
import com.oliveconnect.dto.UserDTO;
import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;
import com.oliveconnect.exception.AccountDisabledException;
import com.oliveconnect.exception.DuplicateEmailException;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.security.CustomUserDetails;
import com.oliveconnect.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Cette adresse e-mail est déjà utilisée.");
        }

        Set<Role> roles = request.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add(Role.AGRICULTEUR);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        List<String> roleNames = savedUser.getRoles().stream().map(Enum::name).toList();
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), roleNames);

        return new AuthResponse(token, UserDTO.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Identifiants invalides.");
        }

        if (!user.isActive()) {
            throw new AccountDisabledException("Votre compte est désactivé ou en attente de validation.");
        }

        List<String> roleNames = user.getRoles().stream().map(Enum::name).toList();
        String token = jwtService.generateToken(user.getId(), user.getEmail(), roleNames);

        return new AuthResponse(token, UserDTO.fromEntity(user));
    }

    public UserDTO getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("Non authentifié.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return UserDTO.fromEntity(userDetails.getUser());
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BadCredentialsException("Utilisateur non trouvé."));
        return UserDTO.fromEntity(user);
    }
}
