package com.oliveconnect.repository;

import com.oliveconnect.entity.Verger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VergerRepository extends MongoRepository<Verger, String> {

    Optional<Verger> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Verger> findByIdAndActifTrue(String id);

    Page<Verger> findByActifTrue(Pageable pageable);

    Page<Verger> findByProprietaireIdAndActifTrue(String proprietaireId, Pageable pageable);

    List<Verger> findByProprietaireIdAndActifTrue(String proprietaireId);

    List<Verger> findByLocalisationNear(Point point, Distance distance);
}
