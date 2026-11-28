package com.oliveconnect.repository;

import com.oliveconnect.entity.Ressource;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.TypeRessource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RessourceRepository extends MongoRepository<Ressource, String> {

    Optional<Ressource> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Ressource> findByIdAndActifTrue(String id);

    List<Ressource> findAllByIdInAndActifTrue(Iterable<String> ids);

    Page<Ressource> findByActifTrue(Pageable pageable);

    Page<Ressource> findByTypeAndActifTrue(TypeRessource type, Pageable pageable);

    Page<Ressource> findByStatutAndActifTrue(StatutRessource statut, Pageable pageable);

    Page<Ressource> findByTypeAndStatutAndActifTrue(TypeRessource type, StatutRessource statut, Pageable pageable);
}
