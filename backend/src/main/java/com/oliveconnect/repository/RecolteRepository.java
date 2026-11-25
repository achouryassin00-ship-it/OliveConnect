package com.oliveconnect.repository;

import com.oliveconnect.entity.Recolte;
import com.oliveconnect.entity.StatutRecolte;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface RecolteRepository extends MongoRepository<Recolte, String> {

    Optional<Recolte> findByReference(String reference);

    Optional<Recolte> findByProprietaireIdAndIdempotencyKey(String proprietaireId, String idempotencyKey);

    boolean existsByVergerIdAndDateSouhaiteeAndStatutIn(
            String vergerId,
            LocalDate dateSouhaitee,
            Collection<StatutRecolte> statuts);

    boolean existsByVergerIdAndStatutIn(String vergerId, Collection<StatutRecolte> statuts);
}
