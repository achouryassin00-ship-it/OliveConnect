package com.oliveconnect.dto;

import com.oliveconnect.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 2, max = 100, message = "Le nom doit comporter entre 2 et 100 caractères.")
    private String name;

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "Le format de l'adresse e-mail est invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 6, max = 100, message = "Le mot de passe doit comporter au moins 6 caractères.")
    private String password;

    private Set<Role> roles;

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String email, String password, Set<Role> roles) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
