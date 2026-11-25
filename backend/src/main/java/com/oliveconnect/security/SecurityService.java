package com.oliveconnect.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {

    public boolean isOwner(String ownerId) {
        if (ownerId == null) return false;
        Authentication authentication = getValidAuthentication();
        if (authentication == null) return false;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return ownerId.equals(userDetails.getId());
        }
        return ownerId.equals(authentication.getName());
    }

    public boolean isOwnerOrAdmin(String ownerId) {
        if (ownerId == null) return false;
        Authentication authentication = getValidAuthentication();
        if (authentication == null) return false;

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRATEUR".equals(a.getAuthority()));
        if (isAdmin) return true;

        return isOwner(ownerId);
    }

    public String getCurrentUserId() {
        Authentication authentication = getValidAuthentication();
        if (authentication == null) return null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        return authentication.getName();
    }

    private Authentication getValidAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }
}
