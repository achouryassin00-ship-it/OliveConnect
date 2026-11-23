package com.oliveconnect;

import com.oliveconnect.dto.ParcelleRequestDTO;
import com.oliveconnect.dto.VergerRequestDTO;
import com.oliveconnect.entity.Parcelle;
import com.oliveconnect.entity.SourcePosition;
import com.oliveconnect.entity.Variete;
import com.oliveconnect.entity.Verger;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.ParcelleRepository;
import com.oliveconnect.repository.VarieteRepository;
import com.oliveconnect.repository.VergerRepository;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.DefaultVergerUsageChecker;
import com.oliveconnect.service.VergerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VergerBusinessRulesTest {

    @Mock
    private VergerRepository vergerRepository;

    @Mock
    private ParcelleRepository parcelleRepository;

    @Mock
    private VarieteRepository varieteRepository;

    @Mock
    private SecurityService securityService;

    private VergerService vergerService;

    @BeforeEach
    void setUp() {
        vergerService = new VergerService(
                vergerRepository,
                parcelleRepository,
                varieteRepository,
                securityService,
                new DefaultVergerUsageChecker(),
                34.30, 35.40, // Sfax lat bounds
                9.80, 11.20,  // Sfax lng bounds
                1000.0        // max surface ha
        );
    }

    @Test
    @DisplayName("VER01_latitudeOutOfRange_returns400 : Latitude hors limites [-90, 90] déclenche ValidationException")
    void testVER01_latitudeOutOfRange_returns400() {
        VergerRequestDTO req = new VergerRequestDTO();
        req.setCode("VRG-ERR-01");
        req.setNom("Verger Lat Invalide");
        req.setLatitude(95.0); // Invalide (> 90)
        req.setLongitude(10.74);
        req.setSurfaceHa(10.0);

        when(vergerRepository.existsByCode("VRG-ERR-01")).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> vergerService.createVerger(req));
        assertNotNull(ex.getFieldErrors());
        assertTrue(ex.getFieldErrors().containsKey("latitude"));
    }

    @Test
    @DisplayName("VER02_outsideSfax_returns400 : Coordonnées hors du gouvernorat de Sfax déclenche ValidationException")
    void testVER02_outsideSfax_returns400() {
        VergerRequestDTO req = new VergerRequestDTO();
        req.setCode("VRG-ERR-02");
        req.setNom("Verger Hors Sfax");
        req.setLatitude(36.80); // Tunis (~36.80, hors Sfax)
        req.setLongitude(10.18);
        req.setSurfaceHa(10.0);

        when(vergerRepository.existsByCode("VRG-ERR-02")).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> vergerService.createVerger(req));
        assertTrue(ex.getMessage().contains("Coordonnées en dehors du gouvernorat de Sfax"));
    }

    @Test
    @DisplayName("VER03_swappedCoordinates_returns400withHint : Coordonnées inversées suggère l'inversion")
    void testVER03_swappedCoordinates_returns400withHint() {
        VergerRequestDTO req = new VergerRequestDTO();
        req.setCode("VRG-ERR-03");
        req.setNom("Verger Inversé");
        // Sfax nominal est lat ~34.80, lng ~10.76.
        // Si l'utilisateur envoie lat = 10.76 et lng = 34.80 (inversion) :
        req.setLatitude(10.76);
        req.setLongitude(34.80);
        req.setSurfaceHa(10.0);

        when(vergerRepository.existsByCode("VRG-ERR-03")).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> vergerService.createVerger(req));
        assertTrue(ex.getMessage().contains("avez-vous inversé la latitude et la longitude"),
                "Le message doit explicitement suggérer l'inversion (VER-03)");
    }

    @Test
    @DisplayName("VER06_negativeSurface_returns400 : Surface négative ou nulle déclenche ValidationException")
    void testVER06_negativeSurface_returns400() {
        VergerRequestDTO req = new VergerRequestDTO();
        req.setCode("VRG-ERR-06");
        req.setNom("Verger Surface Négative");
        req.setLatitude(34.80);
        req.setLongitude(10.76);
        req.setSurfaceHa(-5.0); // Négatif

        when(vergerRepository.existsByCode("VRG-ERR-06")).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> vergerService.createVerger(req));
        assertTrue(ex.getMessage().contains("strictement positive"));
    }

    @Test
    @DisplayName("VER07_parcellesExceedSurface_returns409 : Somme des parcelles dépassant le verger déclenche BusinessConflictException")
    void testVER07_parcellesExceedSurface_returns409() {
        Verger verger = new Verger("vrg-1", "VRG-SFX-001", "Verger Test", "user-1", "El Hencha",
                new GeoJsonPoint(10.74, 35.13), 5.0, SourcePosition.AUTO, 10.0, 100, 2010, true);

        when(vergerRepository.findByIdAndActifTrue("vrg-1")).thenReturn(Optional.of(verger));
        when(securityService.getCurrentUserId()).thenReturn("user-1");

        // Déjà une parcelle de 7.0 ha existante
        Parcelle p1 = new Parcelle("p-1", "vrg-1", "P1", "var-1", 7.0, true);
        when(parcelleRepository.findByVergerIdAndActifTrue("vrg-1")).thenReturn(List.of(p1));

        // Variété active
        Variete var1 = new Variete("var-1", "CHEMLALI", "Chemlali", 0.65, 2.0, true);
        when(varieteRepository.findById("var-1")).thenReturn(Optional.of(var1));

        // Tentative d'ajout d'une parcelle de 4.0 ha (7.0 + 4.0 = 11.0 > 10.0)
        ParcelleRequestDTO newParcelleReq = new ParcelleRequestDTO();
        newParcelleReq.setNom("P2 Excédentaire");
        newParcelleReq.setVarieteId("var-1");
        newParcelleReq.setSurfaceHa(4.0);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> vergerService.createParcelle("vrg-1", newParcelleReq));

        assertTrue(ex.getMessage().contains("dépasse la surface totale du verger"),
                "L'ajout d'une parcelle dépassant la surface totale doit déclencher 409 (VER-07)");
    }

    @Test
    @DisplayName("AUT01_otherFarmerVerger_isDenied : L'accès au verger d'un autre agriculteur est refusé (404 anti-énumération)")
    void testAUT01_otherFarmerVerger_isDenied() {
        Verger verger = new Verger("vrg-victim", "VRG-SFX-099", "Verger Victime", "farmer-victim", "El Hencha",
                new GeoJsonPoint(10.74, 35.13), 5.0, SourcePosition.AUTO, 10.0, 100, 2010, true);

        when(vergerRepository.findByIdAndActifTrue("vrg-victim")).thenReturn(Optional.of(verger));
        when(securityService.getCurrentUserId()).thenReturn("farmer-attacker");

        assertThrows(ResourceNotFoundException.class, () -> vergerService.getVergerById("vrg-victim"),
                "Un agriculteur ne doit pas pouvoir accéder au verger d'un autre agriculteur (AUT-01)");
    }

    @Test
    @DisplayName("AUT02_bodyOwnerIdIgnored : Le proprietaireId est toujours extrait de l'utilisateur authentifié")
    void testAUT02_bodyOwnerIdIgnored() {
        VergerRequestDTO req = new VergerRequestDTO();
        req.setCode("VRG-AUT-02");
        req.setNom("Verger Test Owner");
        req.setLatitude(34.80);
        req.setLongitude(10.76);
        req.setSurfaceHa(12.0);

        when(vergerRepository.existsByCode("VRG-AUT-02")).thenReturn(false);
        when(securityService.getCurrentUserId()).thenReturn("authenticated-farmer-id");
        when(vergerRepository.save(any(Verger.class))).thenAnswer(inv -> {
            Verger saved = inv.getArgument(0);
            saved.setId("vrg-saved-id");
            return saved;
        });

        var res = vergerService.createVerger(req);
        assertNotNull(res);
        assertEquals("authenticated-farmer-id", res.getProprietaireId(),
                "Le proprietaireId doit obligatoirement être l'utilisateur connecté (AUT-02)");
    }
}
