package com.oliveconnect.controller;

import com.oliveconnect.dto.UserDTO;
import com.oliveconnect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Consultation d'un compte utilisateur.
     * AUT-01 & AUT-05: Seul le propriétaire ou un administrateur peut accéder à cette ressource.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityService.isOwnerOrAdmin(#id)")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        UserDTO user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
}
