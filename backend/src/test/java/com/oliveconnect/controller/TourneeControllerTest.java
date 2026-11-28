package com.oliveconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliveconnect.dto.TourneeRequestDTO;
import com.oliveconnect.dto.TourneeResponseDTO;
import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.TourneeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TourneeController.class)
@Import({SecurityConfig.class, SecurityService.class})
public class TourneeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TourneeService tourneeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "col-1", roles = {"RESPONSABLE_COLLECTE"})
    @DisplayName("POST /api/tournees nominal retourne 201 CREATED avec Location")
    void testCreateTourneeNominal() throws Exception {
        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-010");
        req.setDebut(Instant.parse("2026-12-01T08:00:00Z"));
        req.setFin(Instant.parse("2026-12-01T12:00:00Z"));

        TourneeResponseDTO res = new TourneeResponseDTO();
        res.setId("tou-10");
        res.setCode("T-010");
        res.setStatut(StatutTournee.BROUILLON);

        when(tourneeService.createTournee(any(TourneeRequestDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/tournees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tournees/tou-10"))
                .andExpect(jsonPath("$.code").value("T-010"))
                .andExpect(jsonPath("$.statut").value("BROUILLON"));
    }

    @Test
    @WithMockUser(username = "agri-1", roles = {"AGRICULTEUR"})
    @DisplayName("Sécurité : un agriculteur ne peut pas créer ou lire les tournées (403)")
    void testFarmerCannotAccessTournees() throws Exception {
        mockMvc.perform(get("/api/tournees"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "chef-1", roles = {"CHEF_EQUIPE"})
    @DisplayName("Sécurité : un CHEF_EQUIPE accédant à la tournée d'un autre chef reçoit 403")
    void testChefEquipeCannotAccessOtherTournee() throws Exception {
        when(tourneeService.getTourneeById("tou-other"))
                .thenThrow(new AccessDeniedException("Vous ne pouvez consulter que vos propres tournées."));

        mockMvc.perform(get("/api/tournees/tou-other"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "log-1", roles = {"RESPONSABLE_LOGISTIQUE"})
    @DisplayName("POST /api/tournees/{id}/planifier conflit retourne 409")
    void testPlanifierConflictReturns409() throws Exception {
        when(tourneeService.planifierTournee("tou-1", null))
                .thenThrow(new BusinessConflictException("Planification refusée : éléments obligatoires manquants."));

        mockMvc.perform(post("/api/tournees/tou-1/planifier"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }
}
