package com.oliveconnect.repository;

import com.oliveconnect.entity.Variete;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VarieteRepository extends MongoRepository<Variete, String> {

    Optional<Variete> findByCode(String code);

    List<Variete> findByActifTrue();

    boolean existsByCode(String code);
}
