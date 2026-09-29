package com.budgetbuddy.security;

import com.budgetbuddy.auth.User;
import com.budgetbuddy.auth.UserRepository;
import com.budgetbuddy.common.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the authenticated User. Never trusts client-provided user ids:
 * identity always comes from the verified JWT subject.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUserId();
        }
        throw ApiException.forbidden("User not authenticated.");
    }

    public User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> ApiException.forbidden("User account no longer exists."));
    }
}
