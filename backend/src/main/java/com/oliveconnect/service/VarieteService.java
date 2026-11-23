package com.oliveconnect.service;

import com.oliveconnect.dto.VarieteResponseDTO;
import com.oliveconnect.entity.Variete;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.repository.VarieteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VarieteService {

    private final VarieteRepository varieteRepository;

    public VarieteService(VarieteRepository varieteRepository) {
        this.varieteRepository = varieteRepository;
    }

    public List<VarieteResponseDTO> getActiveVarietes() {
        return varieteRepository.findByActifTrue().stream()
                .map(VarieteResponseDTO::fromEntity)
                .toList();
    }

    public Variete getActiveVarieteById(String id) {
        return varieteRepository.findById(id)
                .filter(Variete::isActif)
                .orElseThrow(() -> new ResourceNotFoundException("Variété introuvable ou inactive."));
    }
}
