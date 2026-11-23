package com.oliveconnect.service;

import com.oliveconnect.dto.UserDTO;
import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO findById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant: " + id));
        return UserDTO.fromEntity(user);
    }

    public UserDTO deactivateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant: " + id));

        // CPT-07 : Protection du dernier administrateur actif
        if (user.getRoles().contains(Role.ADMINISTRATEUR) && user.isActive()) {
            long activeAdmins = userRepository.countByRolesContainingAndActiveTrue(Role.ADMINISTRATEUR);
            if (activeAdmins <= 1) {
                throw new IllegalStateException("Impossible de désactiver le dernier administrateur actif de la plateforme.");
            }
        }

        user.setActive(false);
        User updated = userRepository.save(user);
        return UserDTO.fromEntity(updated);
    }
}
