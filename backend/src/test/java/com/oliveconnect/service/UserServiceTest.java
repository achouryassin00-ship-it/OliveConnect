package com.oliveconnect.service;

import com.oliveconnect.dto.UserDTO;
import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("Recherche par ID : utilisateur trouvé")
    void testFindByIdSuccess() {
        User user = new User("u1", "Ali", "ali@sfax.tn", "hash", Set.of(Role.AGRICULTEUR), true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UserDTO dto = userService.findById("u1");

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("u1");
        assertThat(dto.getEmail()).isEqualTo("ali@sfax.tn");
    }

    @Test
    @DisplayName("Recherche par ID inexistant : ResourceNotFoundException levée")
    void testFindByIdNotFound() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    @DisplayName("Désactivation d'un utilisateur standard")
    void testDeactivateUserSuccess() {
        User user = new User("u1", "Ali", "ali@sfax.tn", "hash", Set.of(Role.AGRICULTEUR), true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDTO result = userService.deactivateUser("u1");

        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("CPT-07 : Interdiction de désactiver le dernier administrateur actif")
    void testDeactivateLastAdminThrowsException() {
        User admin = new User("a1", "Admin Principal", "admin@sfax.tn", "hash", Set.of(Role.ADMINISTRATEUR), true);
        when(userRepository.findById("a1")).thenReturn(Optional.of(admin));
        when(userRepository.countByRolesContainingAndActiveTrue(Role.ADMINISTRATEUR)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deactivateUser("a1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dernier administrateur actif");

        verify(userRepository, never()).save(any());
    }
}
