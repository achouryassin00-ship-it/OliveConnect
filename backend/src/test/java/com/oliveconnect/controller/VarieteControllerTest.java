package com.oliveconnect.controller;

import com.oliveconnect.dto.VarieteResponseDTO;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.VarieteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VarieteController.class)
@Import({SecurityConfig.class, SecurityService.class})
public class VarieteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VarieteService varieteService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/varietes sans authentification : retourne 401 UNAUTHORIZED")
    void testGetVarietesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/varietes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("GET /api/varietes authentifié : retourne 200 OK avec la liste des variétés")
    void testGetVarietesAuthenticated() throws Exception {
        VarieteResponseDTO v1 = new VarieteResponseDTO("v-1", "CHEMLALI", "Chemlali de Sfax", 0.65, 2.2, true);
        VarieteResponseDTO v2 = new VarieteResponseDTO("v-2", "CHETOUI", "Chétoui", 0.70, 1.8, true);

        when(varieteService.getActiveVarietes()).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/varietes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CHEMLALI"))
                .andExpect(jsonPath("$[1].code").value("CHETOUI"));
    }
}
