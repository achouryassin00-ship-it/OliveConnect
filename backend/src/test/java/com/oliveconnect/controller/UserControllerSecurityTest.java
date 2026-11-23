package com.oliveconnect.controller;

import com.oliveconnect.dto.UserDTO;
import com.oliveconnect.entity.Role;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityService.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Accès sans authentification à /api/users/{id} : retourne 401 UNAUTHORIZED")
    void testGetUserWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/users/user-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "user-1", roles = {"AGRICULTEUR"})
    @DisplayName("Propriétaire accédant à son propre profil : retourne 200 OK")
    void testOwnerCanAccessOwnProfile() throws Exception {
        UserDTO userDTO = new UserDTO("user-1", "Agriculteur Ali", "ali@sfax.tn", Set.of(Role.AGRICULTEUR), true);
        when(userService.findById("user-1")).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.name").value("Agriculteur Ali"));
    }

    @Test
    @WithMockUser(username = "user-1", roles = {"AGRICULTEUR"})
    @DisplayName("Tentative d'accès aux données d'un autre utilisateur : retourne 403 ACCESS_DENIED")
    void testUserCannotAccessOtherUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/user-2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "admin-1", roles = {"ADMINISTRATEUR"})
    @DisplayName("Administrateur accédant au profil d'un autre utilisateur : retourne 200 OK")
    void testAdminCanAccessAnyProfile() throws Exception {
        UserDTO userDTO = new UserDTO("user-2", "Agriculteur Salah", "salah@sfax.tn", Set.of(Role.AGRICULTEUR), true);
        when(userService.findById("user-2")).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-2"))
                .andExpect(jsonPath("$.name").value("Agriculteur Salah"));
    }
}
