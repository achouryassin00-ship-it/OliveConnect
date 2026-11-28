package com.oliveconnect.controller;

import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RessourceResponseDTO;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.TypeRessource;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.RessourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RessourceController.class)
@Import({SecurityConfig.class, SecurityService.class})
public class RessourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RessourceService ressourceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "log-1", roles = {"RESPONSABLE_LOGISTIQUE"})
    @DisplayName("STA05_statutInconnu_retourne400 : statut inconnu dans JSON retourne 400 avec valeurs acceptées")
    void STA05_statutInconnu_retourne400() throws Exception {
        String body = """
                {
                  "code": "TR-99",
                  "type": "TRACTEUR",
                  "libelle": "Tracteur test",
                  "statut": "STATUT_INCONNU"
                }
                """;

        mockMvc.perform(post("/api/ressources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.statut", containsString("DISPONIBLE")));
    }

    @Test
    @WithMockUser(username = "agri-1", roles = {"AGRICULTEUR"})
    @DisplayName("Sécurité : un agriculteur ne peut pas créer de ressource (403)")
    void testFarmerCannotCreateRessource() throws Exception {
        String body = """
                {
                  "code": "TR-99",
                  "type": "TRACTEUR",
                  "libelle": "Tracteur test"
                }
                """;

        mockMvc.perform(post("/api/ressources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "log-1", roles = {"RESPONSABLE_LOGISTIQUE"})
    @DisplayName("GET /api/ressources nominal retourne 200")
    void testGetRessourcesNominal() throws Exception {
        RessourceResponseDTO dto = new RessourceResponseDTO();
        dto.setId("res-1");
        dto.setCode("TR-04");
        dto.setType(TypeRessource.TRACTEUR);
        dto.setStatut(StatutRessource.DISPONIBLE);

        when(ressourceService.getRessources(any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new PageResponse<>(List.of(dto), 0, 20, 1, 1));

        mockMvc.perform(get("/api/ressources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("TR-04"));
    }
}
