package com.oliveconnect.repository;

import com.oliveconnect.entity.Parcelle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelleRepository extends MongoRepository<Parcelle, String> {

    List<Parcelle> findByVergerIdAndActifTrue(String vergerId);

    List<Parcelle> findByVergerId(String vergerId);

    Optional<Parcelle> findByIdAndActifTrue(String id);

    Optional<Parcelle> findByIdAndVergerIdAndActifTrue(String id, String vergerId);

    Optional<Parcelle> findByVergerIdAndNom(String vergerId, String nom);

    boolean existsByVarieteIdAndActifTrue(String varieteId);
}
