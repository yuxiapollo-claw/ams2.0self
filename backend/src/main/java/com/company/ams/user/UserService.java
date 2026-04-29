package com.company.ams.user;

import com.company.ams.auth.UserPrincipal;
import com.company.ams.common.api.BusinessException;
import com.company.ams.common.persistence.UserRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final String ACCOUNT_STATUS_ENABLED = "ENABLED";
    private static final String ACCOUNT_STATUS_DISABLED = "DISABLED";
    public static final String DEFAULT_RESET_PASSWORD = "ChangeMe123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserRow> list(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (principal.systemAdmin()) {
            return userRepository.findAll();
        }
        return userRepository.findAll().stream()
                .filter(user -> user.id() != null && user.id().equals(principal.id()))
                .toList();
    }

    public UserRow me(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.getRequiredUserRow(principal.id());
    }

    public UserRow create(UserUpsertCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        UserUpsertCommand normalizedCommand = normalizedCommand(command);
        validateUpsert(normalizedCommand);
        if (userRepository.existsUserCode(normalizedCommand.userCode(), null)) {
            throw new BusinessException("User code already exists");
        }
        if (userRepository.existsLoginName(normalizedCommand.loginName(), null)) {
            throw new BusinessException("Login name already exists");
        }
        return userRepository.create(normalizedCommand, passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
    }

    public UserRow update(long userId, UserUpsertCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        UserUpsertCommand normalizedCommand = normalizedCommand(command);
        validateUpsert(normalizedCommand);
        if (userRepository.existsUserCode(normalizedCommand.userCode(), userId)) {
            throw new BusinessException("User code already exists");
        }
        if (userRepository.existsLoginName(normalizedCommand.loginName(), userId)) {
            throw new BusinessException("Login name already exists");
        }
        return userRepository.update(userId, normalizedCommand);
    }

    public UserRow updateStatus(long userId, UserStatusCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.accountStatus() == null || command.accountStatus().isBlank()) {
            throw new IllegalArgumentException("accountStatus is required");
        }
        validateAccountStatus(command.accountStatus());
        return userRepository.updateStatus(userId, command.accountStatus());
    }

    public void delete(long userId, UserPrincipal principal) {
        requireSystemAdmin(principal);
        if (principal.id() == userId) {
            throw new BusinessException("System administrator cannot delete the current login user");
        }
        if (userRepository.existsDeviceAccountsOwnedByUser(userId)) {
            throw new BusinessException("User still owns device accounts");
        }
        if (userRepository.existsUnfinishedRequestsByUser(userId)) {
            throw new BusinessException("User still has unfinished requests");
        }
        userRepository.delete(userId);
    }

    public UserPasswordResetResponse resetPassword(long userId, UserPrincipal principal) {
        requireSystemAdmin(principal);
        userRepository.updatePasswordHash(userId, passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
        return new UserPasswordResetResponse("Password reset completed", DEFAULT_RESET_PASSWORD);
    }

    public UserPasswordResetResponse changeOwnPassword(UserPrincipal principal, UserPasswordChangeCommand command) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (principal.systemAdmin()) {
            throw new AccessDeniedException("System administrator should use the reset password action");
        }
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.currentPassword() == null || command.currentPassword().isBlank()) {
            throw new IllegalArgumentException("currentPassword is required");
        }
        validatePassword(command.newPassword(), "newPassword");
        UserRepository.AuthUserRecord currentUser = userRepository.findAuthUserById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("User " + principal.id() + " does not exist"));
        if (!passwordEncoder.matches(command.currentPassword(), currentUser.passwordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        userRepository.updatePasswordHash(principal.id(), passwordEncoder.encode(command.newPassword().trim()));
        return new UserPasswordResetResponse("Password updated", null);
    }

    private UserUpsertCommand normalizedCommand(UserUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String loginName = command.loginName() == null ? null : command.loginName().trim();
        String derivedUserCode = command.userCode() == null || command.userCode().isBlank()
                ? loginName
                : command.userCode().trim();
        return new UserUpsertCommand(
                derivedUserCode,
                command.userName() == null ? null : command.userName().trim(),
                command.departmentId(),
                command.employmentStatus() == null ? null : command.employmentStatus().trim(),
                loginName,
                command.accountStatus() == null ? null : command.accountStatus().trim());
    }

    private void requireSystemAdmin(UserPrincipal principal) {
        if (principal == null || !principal.systemAdmin()) {
            throw new AccessDeniedException("System administrator access required");
        }
    }

    private void validatePassword(String password, String fieldName) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (password.trim().length() < 8) {
            throw new IllegalArgumentException(fieldName + " must be at least 8 characters");
        }
    }

    public List<UserRow> list() {
        return userRepository.findAll();
    }

    public UserRow create(UserUpsertCommand command) {
        UserUpsertCommand normalizedCommand = normalizedCommand(command);
        validateUpsert(normalizedCommand);
        if (userRepository.existsUserCode(normalizedCommand.userCode(), null)) {
            throw new BusinessException("User code already exists");
        }
        if (userRepository.existsLoginName(normalizedCommand.loginName(), null)) {
            throw new BusinessException("Login name already exists");
        }
        return userRepository.create(normalizedCommand, passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
    }

    public UserRow update(long userId, UserUpsertCommand command) {
        UserUpsertCommand normalizedCommand = normalizedCommand(command);
        validateUpsert(normalizedCommand);
        if (userRepository.existsUserCode(normalizedCommand.userCode(), userId)) {
            throw new BusinessException("User code already exists");
        }
        if (userRepository.existsLoginName(normalizedCommand.loginName(), userId)) {
            throw new BusinessException("Login name already exists");
        }
        return userRepository.update(userId, normalizedCommand);
    }

    public UserRow updateStatus(long userId, UserStatusCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.accountStatus() == null || command.accountStatus().isBlank()) {
            throw new IllegalArgumentException("accountStatus is required");
        }
        validateAccountStatus(command.accountStatus());
        return userRepository.updateStatus(userId, command.accountStatus());
    }

    public void delete(long userId) {
        if (userRepository.existsDeviceAccountsOwnedByUser(userId)) {
            throw new BusinessException("User still owns device accounts");
        }
        if (userRepository.existsUnfinishedRequestsByUser(userId)) {
            throw new BusinessException("User still has unfinished requests");
        }
        userRepository.delete(userId);
    }

    private void validateUpsert(UserUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.userCode() == null || command.userCode().isBlank()) {
            throw new IllegalArgumentException("userCode is required");
        }
        if (command.userName() == null || command.userName().isBlank()) {
            throw new IllegalArgumentException("userName is required");
        }
        if (command.departmentId() == null) {
            throw new IllegalArgumentException("departmentId is required");
        }
        if (!userRepository.existsActiveDepartment(command.departmentId())) {
            throw new IllegalArgumentException("departmentId does not exist");
        }
        if (command.employmentStatus() == null || command.employmentStatus().isBlank()) {
            throw new IllegalArgumentException("employmentStatus is required");
        }
        if (command.loginName() == null || command.loginName().isBlank()) {
            throw new IllegalArgumentException("loginName is required");
        }
        if (command.accountStatus() == null || command.accountStatus().isBlank()) {
            throw new IllegalArgumentException("accountStatus is required");
        }
        validateAccountStatus(command.accountStatus());
    }

    private void validateAccountStatus(String accountStatus) {
        if (!ACCOUNT_STATUS_ENABLED.equals(accountStatus) && !ACCOUNT_STATUS_DISABLED.equals(accountStatus)) {
            throw new IllegalArgumentException("accountStatus must be ENABLED or DISABLED");
        }
    }
}
