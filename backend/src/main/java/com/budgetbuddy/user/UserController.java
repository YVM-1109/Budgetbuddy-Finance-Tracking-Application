package com.budgetbuddy.user;

import com.budgetbuddy.auth.User;
import com.budgetbuddy.auth.UserRepository;
import com.budgetbuddy.auth.UserResponse;
import com.budgetbuddy.common.error.ApiException;
import com.budgetbuddy.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Profile endpoints. Only name, monthly budget and monthly savings target
 * are editable; email and provider identity are not.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public UserController(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    public record UpdateProfileRequest(
            @Size(max = 120, message = "Name must be at most 120 characters.")
            String name,
            @DecimalMin(value = "0", inclusive = true, message = "Monthly budget must be zero or greater.")
            BigDecimal monthlyBudget,
            @DecimalMin(value = "0", inclusive = true, message = "Monthly savings target must be zero or greater.")
            BigDecimal monthlySavingsTarget
    ) {
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUserService.currentUser());
    }

    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUserService.currentUser();
        if (request.name() != null) {
            String trimmed = request.name().trim();
            if (trimmed.isEmpty()) {
                throw ApiException.badRequest("Name cannot be empty.");
            }
            user.setName(trimmed);
        }
        if (request.monthlyBudget() != null) {
            user.setMonthlyBudget(request.monthlyBudget());
        }
        if (request.monthlySavingsTarget() != null) {
            user.setMonthlySavingsTarget(request.monthlySavingsTarget());
        }
        return UserResponse.from(userRepository.save(user));
    }
}
