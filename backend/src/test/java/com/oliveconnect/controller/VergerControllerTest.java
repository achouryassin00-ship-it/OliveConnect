package com.oliveconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliveconnect.dto.*;
import com.oliveconnect.entity.SourcePosition;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.VergerService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VergerController.class)
@Import({SecurityConfig.class, SecurityService.class})
public class VergerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VergerService vergerService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/vergers sans authentification : retourne 401 UNAUTHORIZED")
    void testGetVergersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/vergers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("GET /api/vergers nominal : retourne 200 OK avec PageResponse")
    void testGetVergersNominal() throws Exception {
        VergerResponseDTO v1 = new VergerResponseDTO();
        v1.setId("vrg-1");
        v1.setCode("VRG-SFX-001");
        v1.setNom("Verger El Hencha Nord");
        v1.setSurfaceHa(15.5);
        v1.setLongitude(10.74);
        v1.setLatitude(35.13);

        PageResponse<VergerResponseDTO> pageResponse = new PageResponse<>(List.of(v1), 0, 20, 1, 1);
        when(vergerService.getVergers(0, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/vergers?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("VRG-SFX-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("GET /api/vergers/{id} nominal : retourne 200 OK")
    void testGetVergerByIdNominal() throws Exception {
        VergerResponseDTO v1 = new VergerResponseDTO();
        v1.setId("vrg-1");
        v1.setCode("VRG-SFX-001");
        v1.setNom("Verger El Hencha Nord");
        when(vergerService.getVergerById("vrg-1")).thenReturn(v1);

        mockMvc.perform(get("/api/vergers/vrg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("vrg-1"))
                .andExpect(jsonPath("$.code").value("VRG-SFX-001"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("GET /api/vergers/{id} non trouvé ou refusé (AUT-01) : retourne 404 RESOURCE_NOT_FOUND")
    void testGetVergerByIdNotFound() throws Exception {
        when(vergerService.getVergerById("vrg-unknown"))
                .thenThrow(new ResourceNotFoundException("Verger introuvable."));

        mockMvc.perform(get("/api/vergers/vrg-unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/vergers nominal : retourne 201 CREATED avec Location")
    void testCreateVergerNominal() throws Exception {
        VergerRequestDTO request = new VergerRequestDTO();
        request.setCode("VRG-SFX-010");
        request.setNom("Nouveau Verger Sfax");
        request.setLatitude(34.80);
        request.setLongitude(10.76);
        request.setSurfaceHa(12.0);
        request.setSourcePosition(SourcePosition.AUTO);

        VergerResponseDTO response = new VergerResponseDTO();
        response.setId("vrg-new-10");
        response.setCode("VRG-SFX-010");
        response.setNom("Nouveau Verger Sfax");

        when(vergerService.createVerger(any(VergerRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/vergers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/vergers/vrg-new-10"))
                .andExpect(jsonPath("$.id").value("vrg-new-10"))
                .andExpect(jsonPath("$.code").value("VRG-SFX-010"));
    }

    @Test
    @WithMockUser(username = "collecte-1", roles = {"RESPONSABLE_COLLECTE"})
    @DisplayName("POST /api/vergers sans rôle AGRICULTEUR : retourne 403 ACCESS_DENIED")
    void testCreateVergerForbiddenForNonFarmer() throws Exception {
        VergerRequestDTO request = new VergerRequestDTO();
        request.setCode("VRG-SFX-011");
        request.setNom("Verger Staff");
        request.setLatitude(34.80);
        request.setLongitude(10.76);
        request.setSurfaceHa(12.0);

        mockMvc.perform(post("/api/vergers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/vergers coordonnées hors Sfax (VER-02) : retourne 400 VALIDATION_ERROR")
    void testCreateVergerOutsideSfax() throws Exception {
        VergerRequestDTO request = new VergerRequestDTO();
        request.setCode("VRG-SFX-ERR");
        request.setNom("Verger Hors Sfax");
        request.setLatitude(36.80);
        request.setLongitude(10.18);
        request.setSurfaceHa(12.0);

        when(vergerService.createVerger(any(VergerRequestDTO.class)))
                .thenThrow(new ValidationException("Coordonnées en dehors du gouvernorat de Sfax."));

        mockMvc.perform(post("/api/vergers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Coordonnées en dehors du gouvernorat de Sfax."));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("PUT /api/vergers/{id} nominal : retourne 200 OK")
    void testUpdateVergerNominal() throws Exception {
        VergerRequestDTO request = new VergerRequestDTO();
        request.setCode("VRG-SFX-001");
        request.setNom("Verger Modifié");
        request.setLatitude(35.13);
        request.setLongitude(10.74);
        request.setSurfaceHa(16.0);

        VergerResponseDTO response = new VergerResponseDTO();
        response.setId("vrg-1");
        response.setCode("VRG-SFX-001");
        response.setNom("Verger Modifié");

        when(vergerService.updateVerger(eq("vrg-1"), any(VergerRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/vergers/vrg-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Verger Modifié"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("DELETE /api/vergers/{id} nominal : retourne 204 NO CONTENT")
    void testDeleteVergerNominal() throws Exception {
        doNothing().when(vergerService).deleteVerger("vrg-1");

        mockMvc.perform(delete("/api/vergers/vrg-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/vergers/{id}/parcelles nominal : retourne 201 CREATED")
    void testCreateParcelleNominal() throws Exception {
        ParcelleRequestDTO request = new ParcelleRequestDTO();
        request.setNom("Parcelle A");
        request.setVarieteId("var-chemlali");
        request.setSurfaceHa(5.0);

        ParcelleResponseDTO response = new ParcelleResponseDTO();
        response.setId("parc-1");
        response.setVergerId("vrg-1");
        response.setNom("Parcelle A");
        response.setVarieteId("var-chemlali");
        response.setSurfaceHa(5.0);

        when(vergerService.createParcelle(eq("vrg-1"), any(ParcelleRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/vergers/vrg-1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/vergers/vrg-1/parcelles/parc-1"))
                .andExpect(jsonPath("$.id").value("parc-1"))
                .andExpect(jsonPath("$.nom").value("Parcelle A"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/vergers/{id}/parcelles dépassement surface (VER-07) : retourne 409 RESOURCE_CONFLICT")
    void testCreateParcelleSurfaceExceeded() throws Exception {
        ParcelleRequestDTO request = new ParcelleRequestDTO();
        request.setNom("Parcelle Trop Grande");
        request.setVarieteId("var-chemlali");
        request.setSurfaceHa(50.0);

        when(vergerService.createParcelle(eq("vrg-1"), any(ParcelleRequestDTO.class)))
                .thenThrow(new BusinessConflictException("La somme des surfaces dépasse la surface totale du verger."));

        mockMvc.perform(post("/api/vergers/vrg-1/parcelles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("DELETE /api/vergers/{id}/parcelles/{parcelleId} : retourne 204 NO CONTENT")
    void testDeleteParcelleNominal() throws Exception {
        doNothing().when(vergerService).deleteParcelle("vrg-1", "parc-1");

        mockMvc.perform(delete("/api/vergers/vrg-1/parcelles/parc-1"))
                .andExpect(status().isNoContent());
    }
}
