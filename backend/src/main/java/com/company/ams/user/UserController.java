package com.company.ams.user;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import com.company.ams.auth.AuthenticatedUserResolver;
import com.company.ams.auth.UserPrincipal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<ListPayload<UserRow>> list(Authentication authentication) {
        List<UserRow> users = AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? userService.list(principal(authentication))
                : userService.list();
        return ApiResponse.success(new ListPayload<>(users, users.size()));
    }

    @GetMapping("/me")
    public ApiResponse<UserRow> me(Authentication authentication) {
        return ApiResponse.success(userService.me(principal(authentication)));
    }

    @PostMapping
    public ApiResponse<UserRow> create(@RequestBody UserUpsertCommand command, Authentication authentication) {
        return ApiResponse.success(AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? userService.create(command, principal(authentication))
                : userService.create(command));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserRow> update(
            @PathVariable long userId,
            @RequestBody UserUpsertCommand command,
            Authentication authentication) {
        return ApiResponse.success(AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? userService.update(userId, command, principal(authentication))
                : userService.update(userId, command));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserRow> patchStatus(
            @PathVariable long userId,
            @RequestBody UserStatusCommand command,
            Authentication authentication) {
        return ApiResponse.success(AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? userService.updateStatus(userId, command, principal(authentication))
                : userService.updateStatus(userId, command));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<UserPasswordResetResponse> resetPassword(
            @PathVariable long userId,
            Authentication authentication) {
        return ApiResponse.success(userService.resetPassword(userId, principal(authentication)));
    }

    @PostMapping("/change-password")
    public ApiResponse<UserPasswordResetResponse> changePassword(
            @RequestBody UserPasswordChangeCommand command,
            Authentication authentication) {
        return ApiResponse.success(userService.changeOwnPassword(principal(authentication), command));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable long userId, Authentication authentication) {
        if (AuthenticatedUserResolver.hasNativePrincipal(authentication)) {
            userService.delete(userId, principal(authentication));
        } else {
            userService.delete(userId);
        }
        return ApiResponse.success(null);
    }

    private UserPrincipal principal(Authentication authentication) {
        return AuthenticatedUserResolver.resolve(authentication);
    }
}
