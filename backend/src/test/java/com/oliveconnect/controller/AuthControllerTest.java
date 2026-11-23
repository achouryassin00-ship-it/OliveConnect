package com.oliveconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliveconnect.dto.AuthResponse;
import com.oliveconnect.dto.LoginRequest;
import com.oliveconnect.dto.RegisterRequest;
import com.oliveconnect.dto.UserDTO;
import com.oliveconnect.entity.Role;
import com.oliveconnect.exception.AccountDisabledException;
import com.oliveconnect.exception.DuplicateEmailException;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Inscription valide : retourne 201 avec en-tête Location et jeton")
    void testRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("Yassine Ben", "yassine@sfax.tn", "password123", Set.of(Role.AGRICULTEUR));
        UserDTO userDto = new UserDTO("user-1", "Yassine Ben", "yassine@sfax.tn", Set.of(Role.AGRICULTEUR), true);
        AuthResponse authResponse = new AuthResponse("jwt-token-123", userDto);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/user-1"))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.user.email").value("yassine@sfax.tn"));
    }

    @Test
    @DisplayName("Inscription avec e-mail dupliqué : retourne 409 EMAIL_ALREADY_EXISTS")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest req = new RegisterRequest("Yassine Ben", "yassine@sfax.tn", "password123", null);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("Cette adresse e-mail est déjà utilisée."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Cette adresse e-mail est déjà utilisée."));
    }

    @Test
    @DisplayName("Données invalides à l'inscription : retourne 400 VALIDATION_ERROR avec fieldErrors")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest invalidReq = new RegisterRequest("", "invalid-email-format", "123", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Connexion valide : retourne 200 avec jeton d'authentification")
    void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("ali@sfax.tn", "password123");
        UserDTO userDto = new UserDTO("user-2", "Ali", "ali@sfax.tn", Set.of(Role.AGRICULTEUR), true);
        AuthResponse authResponse = new AuthResponse("jwt-token-456", userDto);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-456"))
                .andExpect(jsonPath("$.user.id").value("user-2"));
    }

    @Test
    @DisplayName("Connexion avec mot de passe incorrect : retourne 401 BAD_CREDENTIALS")
    void testLoginBadCredentials() throws Exception {
        LoginRequest req = new LoginRequest("ali@sfax.tn", "wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Identifiants invalides."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    @DisplayName("Connexion avec compte inactif : retourne 403 ACCOUNT_DISABLED")
    void testLoginAccountDisabled() throws Exception {
        LoginRequest req = new LoginRequest("ali@sfax.tn", "password123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccountDisabledException("Votre compte est désactivé."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("GET /api/auth/me sans authentification : retourne 401 UNAUTHORIZED")
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "ali@sfax.tn", roles = {"AGRICULTEUR"})
    @DisplayName("GET /api/auth/me avec utilisateur connecté : retourne 200 et UserDTO")
    void testGetMeAuthorized() throws Exception {
        UserDTO userDto = new UserDTO("user-2", "Ali", "ali@sfax.tn", Set.of(Role.AGRICULTEUR), true);
        when(authService.getCurrentUser()).thenReturn(userDto);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ali@sfax.tn"));
    }

    @Test
    @DisplayName("POST /api/auth/logout : retourne 204 No Content")
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
