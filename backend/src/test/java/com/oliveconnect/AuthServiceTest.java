package com.oliveconnect;

import com.oliveconnect.dto.AuthResponse;
import com.oliveconnect.dto.LoginRequest;
import com.oliveconnect.dto.RegisterRequest;
import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;
import com.oliveconnect.exception.AccountDisabledException;
import com.oliveconnect.exception.DuplicateEmailException;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Inscription valide : mot de passe haché en BCrypt et compte actif")
    void testRegisterSuccessAndPasswordHashed() {
        RegisterRequest req = new RegisterRequest("Ali Ben", "ali@sfax.tn", "secret123", Set.of(Role.AGRICULTEUR));
        when(userRepository.existsByEmail("ali@sfax.tn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("user-1");
            return u;
        });
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("mock-token");

        AuthResponse resp = authService.register(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("mock-token");
        assertThat(resp.getUser().getEmail()).isEqualTo("ali@sfax.tn");
        assertThat(resp.getUser().isActive()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("secret123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Inscription avec e-mail déjà existant : DuplicateEmailException levée")
    void testRegisterDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("Ali Ben", "  ALI@SFAX.TN ", "secret123", null);
        when(userRepository.existsByEmail("ali@sfax.tn")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Connexion valide : retour du jeton et des informations de l'utilisateur")
    void testLoginSuccess() {
        String hash = passwordEncoder.encode("secret123");
        User user = new User("u1", "Ali", "ali@sfax.tn", hash, Set.of(Role.AGRICULTEUR), true);

        when(userRepository.findByEmail("ali@sfax.tn")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("jwt-123");

        LoginRequest req = new LoginRequest("ali@sfax.tn", "secret123");
        AuthResponse resp = authService.login(req);

        assertThat(resp.getToken()).isEqualTo("jwt-123");
        assertThat(resp.getUser().getId()).isEqualTo("u1");
    }

    @Test
    @DisplayName("Connexion avec mot de passe incorrect : BadCredentialsException")
    void testLoginWrongPassword() {
        String hash = passwordEncoder.encode("secret123");
        User user = new User("u1", "Ali", "ali@sfax.tn", hash, Set.of(Role.AGRICULTEUR), true);

        when(userRepository.findByEmail("ali@sfax.tn")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest("ali@sfax.tn", "wrongpass");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides.");
    }

    @Test
    @DisplayName("Connexion avec compte inactif : AccountDisabledException")
    void testLoginDisabledAccount() {
        String hash = passwordEncoder.encode("secret123");
        User user = new User("u1", "Ali", "ali@sfax.tn", hash, Set.of(Role.AGRICULTEUR), false);

        when(userRepository.findByEmail("ali@sfax.tn")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest("ali@sfax.tn", "secret123");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    @DisplayName("Sécurité : l'inscription publique refuse un rôle privilégié (ADMINISTRATEUR)")
    void testRegisterRejectsPrivilegedRole() {
        RegisterRequest req = new RegisterRequest("Malicious User", "hacker@sfax.tn", "secret123", Set.of(Role.ADMINISTRATEUR));
        when(userRepository.existsByEmail("hacker@sfax.tn")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sécurité : l'inscription publique refuse le cumul AGRICULTEUR + RESPONSABLE_COLLECTE")
    void testRegisterRejectsMixedPrivilegedRole() {
        RegisterRequest req = new RegisterRequest(
                "Malicious User",
                "hacker2@sfax.tn",
                "secret123",
                Set.of(Role.AGRICULTEUR, Role.RESPONSABLE_COLLECTE)
        );
        when(userRepository.existsByEmail("hacker2@sfax.tn")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Inscription sans rôle explicite : attribue uniquement AGRICULTEUR")
    void testRegisterDefaultsOnlyToAgriculteur() {
        RegisterRequest req = new RegisterRequest("Salma Ben", "salma@sfax.tn", "secret123", null);
        when(userRepository.existsByEmail("salma@sfax.tn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("user-salma");
            return u;
        });
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("mock-token");

        AuthResponse resp = authService.register(req);
        assertThat(resp.getUser().getRoles()).containsExactly(Role.AGRICULTEUR);
    }
}
