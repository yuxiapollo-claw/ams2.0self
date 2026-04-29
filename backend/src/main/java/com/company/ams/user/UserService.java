package com.company.ams.user;

import com.company.ams.common.api.BusinessException;
import com.company.ams.common.persistence.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final String ACCOUNT_STATUS_ENABLED = "ENABLED";
    private static final String ACCOUNT_STATUS_DISABLED = "DISABLED";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserRow> list() {
        return userRepository.findAll();
    }

    public UserRow create(UserUpsertCommand command) {
        validateUpsert(command);
        if (userRepository.existsUserCode(command.userCode(), null)) {
            throw new BusinessException("User code already exists");
        }
        if (userRepository.existsLoginName(command.loginName(), null)) {
            throw new BusinessException("Login name already exists");
        }
        return userRepository.create(command);
    }

    public UserRow update(long userId, UserUpsertCommand command) {
        validateUpsert(command);
        if (userRepository.existsUserCode(command.userCode(), userId)) {
            throw new BusinessException("User code already exists");
        }
        if (userRepository.existsLoginName(command.loginName(), userId)) {
            throw new BusinessException("Login name already exists");
        }
        return userRepository.update(userId, command);
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
