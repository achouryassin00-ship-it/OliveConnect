package com.oliveconnect.service;

import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RessourceRequestDTO;
import com.oliveconnect.dto.RessourceResponseDTO;
import com.oliveconnect.entity.Ressource;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.entity.Tournee;
import com.oliveconnect.entity.TypeRessource;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.RessourceRepository;
import com.oliveconnect.repository.TourneeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RessourceServiceTest {

    @Mock
    private RessourceRepository ressourceRepository;

    @Mock
    private TourneeRepository tourneeRepository;

    private RessourceService ressourceService;

    @BeforeEach
    void setUp() {
        ressourceService = new RessourceService(ressourceRepository, tourneeRepository);
    }

    @Test
    @DisplayName("INT01_suppressionRessourceAffectee_retourne409 : refuser suppression d'un tracteur affecté à une tournée PLANIFIEE/EN_COURS")
    void INT01_suppressionRessourceAffectee_retourne409() {
        Ressource tracteur = new Ressource("res-tr-04", "TR-04", TypeRessource.TRACTEUR,
                "New Holland T5", StatutRessource.DISPONIBLE, null, true);

        Tournee tournee = new Tournee();
        tournee.setId("tou-1");
        tournee.setCode("T-001");
        tournee.setStatut(StatutTournee.PLANIFIEE);
        tournee.setTracteurIds(List.of("res-tr-04"));

        when(ressourceRepository.findByIdAndActifTrue("res-tr-04")).thenReturn(Optional.of(tracteur));
        when(tourneeRepository.findByActifTrueAndStatutInAndTracteurIdsIn(
                StatutTournee.statutsBlocageRessource(), Set.of("res-tr-04")))
                .thenReturn(List.of(tournee));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> ressourceService.deleteRessource("res-tr-04"));

        assertTrue(ex.getMessage().contains("Impossible de supprimer : 1 tournée(s) utilisent cette ressource"));
        assertTrue(ex.getMessage().contains("T-001"));
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("TOU10_horsServiceRessourceAffectee_retourne409 : refuser passage HORS_SERVICE si affectée à une tournée à venir")
    void TOU10_horsServiceRessourceAffectee_retourne409() {
        Ressource tracteur = new Ressource("res-tr-04", "TR-04", TypeRessource.TRACTEUR,
                "New Holland T5", StatutRessource.DISPONIBLE, null, true);
        tracteur.setVersion(0L);

        Tournee tournee = new Tournee();
        tournee.setId("tou-1");
        tournee.setCode("T-001");
        tournee.setStatut(StatutTournee.PLANIFIEE);

        when(ressourceRepository.findByIdAndActifTrue("res-tr-04")).thenReturn(Optional.of(tracteur));
        when(tourneeRepository.findByActifTrueAndStatutInAndTracteurIdsIn(
                StatutTournee.statutsBlocageRessource(), Set.of("res-tr-04")))
                .thenReturn(List.of(tournee));

        RessourceRequestDTO req = new RessourceRequestDTO();
        req.setCode("TR-04");
        req.setType(TypeRessource.TRACTEUR);
        req.setLibelle("New Holland T5");
        req.setStatut(StatutRessource.HORS_SERVICE);
        req.setVersion(0L);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> ressourceService.updateRessource("res-tr-04", req));

        assertTrue(ex.getMessage().contains("HORS_SERVICE"));
        assertTrue(ex.getMessage().contains("T-001"));
    }

    @Test
    @DisplayName("INT03_ressourceInactive_absenteDesListes : les listes utilisent findByActifTrue")
    void INT03_ressourceInactive_absenteDesListes() {
        Ressource active = new Ressource("res-1", "TR-01", TypeRessource.TRACTEUR,
                "Tracteur 1", StatutRessource.DISPONIBLE, null, true);

        when(ressourceRepository.findByActifTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active)));

        PageResponse<RessourceResponseDTO> page = ressourceService.getRessources(null, null, 0, 20);
        assertEquals(1, page.getItems().size());
        assertEquals("TR-01", page.getItems().get(0).getCode());
        verify(ressourceRepository).findByActifTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("BENNE sans capacité > 0 : retourne ValidationException (400)")
    void testBenneRequiresPositiveCapacite() {
        RessourceRequestDTO req = new RessourceRequestDTO();
        req.setCode("BN-01");
        req.setType(TypeRessource.BENNE);
        req.setLibelle("Benne 2T");
        req.setCapaciteKg(0.0);

        when(ressourceRepository.existsByCode("BN-01")).thenReturn(false);

        assertThrows(ValidationException.class, () -> ressourceService.createRessource(req));
    }
}
