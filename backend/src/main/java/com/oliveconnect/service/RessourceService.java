package com.oliveconnect.service;

import com.oliveconnect.dto.CalendrierRessourceDTO;
import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RessourceRequestDTO;
import com.oliveconnect.dto.RessourceResponseDTO;
import com.oliveconnect.entity.Ressource;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.entity.Tournee;
import com.oliveconnect.entity.TypeRessource;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.RessourceRepository;
import com.oliveconnect.repository.TourneeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RessourceService {

    private final RessourceRepository ressourceRepository;
    private final TourneeRepository tourneeRepository;

    public RessourceService(RessourceRepository ressourceRepository,
                            TourneeRepository tourneeRepository) {
        this.ressourceRepository = ressourceRepository;
        this.tourneeRepository = tourneeRepository;
    }

    public PageResponse<RessourceResponseDTO> getRessources(TypeRessource type, StatutRessource statut, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by("code").ascending());

        Page<Ressource> result;
        if (type != null && statut != null) {
            result = ressourceRepository.findByTypeAndStatutAndActifTrue(type, statut, pageable);
        } else if (type != null) {
            result = ressourceRepository.findByTypeAndActifTrue(type, pageable);
        } else if (statut != null) {
            result = ressourceRepository.findByStatutAndActifTrue(statut, pageable);
        } else {
            result = ressourceRepository.findByActifTrue(pageable);
        }

        List<RessourceResponseDTO> items = result.getContent().stream()
                .map(RessourceResponseDTO::fromEntity)
                .toList();

        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public RessourceResponseDTO getRessourceById(String id) {
        Ressource ressource = findActiveOrThrow(id);
        return RessourceResponseDTO.fromEntity(ressource);
    }

    public RessourceResponseDTO createRessource(RessourceRequestDTO request) {
        String cleanCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (ressourceRepository.existsByCode(cleanCode)) {
            throw new BusinessConflictException("Une ressource avec le code " + cleanCode + " existe déjà.");
        }

        validateCapacite(request.getType(), request.getCapaciteKg());

        StatutRessource statut = request.getStatut() != null ? request.getStatut() : StatutRessource.DISPONIBLE;
        Double capacite = request.getType() == TypeRessource.BENNE ? request.getCapaciteKg() : null;

        Ressource ressource = new Ressource(
                null,
                cleanCode,
                request.getType(),
                request.getLibelle().trim(),
                statut,
                capacite,
                true
        );

        return RessourceResponseDTO.fromEntity(ressourceRepository.save(ressource));
    }

    public RessourceResponseDTO updateRessource(String id, RessourceRequestDTO request) {
        Ressource ressource = findActiveOrThrow(id);

        if (request.getVersion() != null && !request.getVersion().equals(ressource.getVersion())) {
            throw new BusinessConflictException("Conflit de version : la ressource a été modifiée par un autre utilisateur.");
        }

        validateCapacite(request.getType(), request.getCapaciteKg());

        StatutRessource nouveauStatut = request.getStatut() != null ? request.getStatut() : ressource.getStatut();
        if (nouveauStatut == StatutRessource.HORS_SERVICE || nouveauStatut == StatutRessource.MAINTENANCE) {
            List<Tournee> tourneesBloquees = findTourneesEngagees(ressource.getId(), ressource.getType());
            if (!tourneesBloquees.isEmpty()) {
                String codes = tourneesBloquees.stream().map(Tournee::getCode).sorted().reduce((a, b) -> a + ", " + b).orElse("");
                throw new BusinessConflictException(
                        "Impossible de passer cette ressource en " + nouveauStatut + " : elle est affectée aux tournées "
                                + codes + ". Retirez-la d'abord de ces tournées."
                );
            }
        }

        // Code immuable (§7.3)
        ressource.setType(request.getType());
        ressource.setLibelle(request.getLibelle().trim());
        ressource.setStatut(nouveauStatut);
        ressource.setCapaciteKg(request.getType() == TypeRessource.BENNE ? request.getCapaciteKg() : null);

        return RessourceResponseDTO.fromEntity(ressourceRepository.save(ressource));
    }

    public void deleteRessource(String id) {
        Ressource ressource = findActiveOrThrow(id);

        List<Tournee> tourneesEngagees = findTourneesEngagees(ressource.getId(), ressource.getType());
        if (!tourneesEngagees.isEmpty()) {
            String codes = tourneesEngagees.stream().map(Tournee::getCode).sorted().reduce((a, b) -> a + ", " + b).orElse("");
            throw new BusinessConflictException(
                    "Impossible de supprimer : " + tourneesEngagees.size() + " tournée(s) utilisent cette ressource ("
                            + codes + "). Retirez-la d'abord de ces tournées."
            );
        }

        ressource.setActif(false);
        ressourceRepository.save(ressource);
    }

    public CalendrierRessourceDTO getCalendrierRessource(String id, Instant debut, Instant fin) {
        Ressource ressource = findActiveOrThrow(id);
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new ValidationException(
                    "La fenêtre de dates est invalide : la date de fin doit être postérieure au début.",
                    Map.of("fin", "La date de fin doit être postérieure au début.")
            );
        }

        List<Tournee> tournees = findTourneesParStatuts(
                ressource.getId(),
                ressource.getType(),
                StatutTournee.statutsActifsChevauchement()
        );

        List<CalendrierRessourceDTO.CreneauTourneeDTO> creneaux = tournees.stream()
                .filter(t -> debut == null || !t.getFin().isBefore(debut))
                .filter(t -> fin == null || !t.getDebut().isAfter(fin))
                .sorted(Comparator.comparing(Tournee::getDebut))
                .map(t -> new CalendrierRessourceDTO.CreneauTourneeDTO(
                        t.getId(), t.getCode(), t.getDebut(), t.getFin(), t.getStatut()))
                .toList();

        return new CalendrierRessourceDTO(
                ressource.getId(),
                ressource.getCode(),
                ressource.getLibelle(),
                ressource.getType(),
                ressource.getStatut(),
                creneaux
        );
    }

    private Ressource findActiveOrThrow(String id) {
        return ressourceRepository.findByIdAndActifTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable."));
    }

    private void validateCapacite(TypeRessource type, Double capaciteKg) {
        if (type == TypeRessource.BENNE) {
            if (capaciteKg == null || !Double.isFinite(capaciteKg) || capaciteKg <= 0.0) {
                throw new ValidationException(
                        "La capacité en kg est obligatoire et doit être strictement positive pour une benne.",
                        Map.of("capaciteKg", "Indiquez une capacité strictement positive en kg.")
                );
            }
        }
    }

    private List<Tournee> findTourneesEngagees(String ressourceId, TypeRessource type) {
        return findTourneesParStatuts(ressourceId, type, StatutTournee.statutsBlocageRessource());
    }

    private List<Tournee> findTourneesParStatuts(String ressourceId, TypeRessource type, Collection<StatutTournee> statuts) {
        Set<String> ids = Set.of(ressourceId);
        return switch (type) {
            case TRACTEUR -> tourneeRepository.findByActifTrueAndStatutInAndTracteurIdsIn(statuts, ids);
            case BENNE -> tourneeRepository.findByActifTrueAndStatutInAndBenneIdsIn(statuts, ids);
            case OUVRIER -> tourneeRepository.findByActifTrueAndStatutInAndOuvrierIdsIn(statuts, ids);
        };
    }
}
