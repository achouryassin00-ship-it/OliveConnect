package com.oliveconnect.dto;

import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;

import java.util.Set;

public class UserDTO {

    private String id;
    private String name;
    private String email;
    private Set<Role> roles;
    private boolean active;

    public UserDTO() {
    }

    public UserDTO(String id, String name, String email, Set<Role> roles, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.active = active;
    }

    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.isActive()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
