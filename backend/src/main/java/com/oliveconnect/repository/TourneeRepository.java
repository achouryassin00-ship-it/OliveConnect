package com.oliveconnect.repository;

import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.entity.Tournee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourneeRepository extends MongoRepository<Tournee, String> {

    Optional<Tournee> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Tournee> findByIdAndActifTrue(String id);

    Page<Tournee> findByActifTrue(Pageable pageable);

    Page<Tournee> findByActifTrueAndChefEquipeId(String chefEquipeId, Pageable pageable);

    Page<Tournee> findByActifTrueAndDebutLessThanEqualAndFinGreaterThanEqual(
            Instant finFenetre, Instant debutFenetre, Pageable pageable);

    Page<Tournee> findByActifTrueAndChefEquipeIdAndDebutLessThanEqualAndFinGreaterThanEqual(
            String chefEquipeId, Instant finFenetre, Instant debutFenetre, Pageable pageable);

    List<Tournee> findByActifTrueAndStatutInAndTracteurIdsIn(
            Collection<StatutTournee> statuts, Collection<String> tracteurIds);

    List<Tournee> findByActifTrueAndStatutInAndBenneIdsIn(
            Collection<StatutTournee> statuts, Collection<String> benneIds);

    List<Tournee> findByActifTrueAndStatutInAndOuvrierIdsIn(
            Collection<StatutTournee> statuts, Collection<String> ouvrierIds);

    List<Tournee> findByActifTrueAndStatutInAndRecolteIdsIn(
            Collection<StatutTournee> statuts, Collection<String> recolteIds);

    boolean existsByActifTrueAndStatutInAndRecolteIdsContaining(
            Collection<StatutTournee> statuts, String recolteId);
}
