package com.company.ams.account;

import com.company.ams.common.api.BusinessException;
import com.company.ams.common.persistence.DeviceAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeviceAccountService {
    private static final String ACCOUNT_STATUS_ENABLED = "ENABLED";
    private static final String ACCOUNT_STATUS_DISABLED = "DISABLED";

    private final DeviceAccountRepository deviceAccountRepository;

    public DeviceAccountService(DeviceAccountRepository deviceAccountRepository) {
        this.deviceAccountRepository = deviceAccountRepository;
    }

    public List<DeviceAccountRow> list() {
        return deviceAccountRepository.findAll();
    }

    public List<DeviceAccountRow> byDevice(Long deviceNodeId) {
        if (deviceNodeId == null) {
            throw new IllegalArgumentException("deviceNodeId is required");
        }
        return deviceAccountRepository.findByDeviceNodeId(deviceNodeId);
    }

    public DeviceAccountRow create(DeviceAccountUpsertCommand command) {
        validateUpsert(command);
        if (deviceAccountRepository.existsByDeviceAndAccountName(command.deviceNodeId(), command.accountName(), null)) {
            throw new BusinessException("Device account already exists on this device");
        }
        return deviceAccountRepository.create(command);
    }

    public DeviceAccountRow update(long deviceAccountId, DeviceAccountUpsertCommand command) {
        validateUpsert(command);
        if (deviceAccountRepository.existsByDeviceAndAccountName(command.deviceNodeId(), command.accountName(), deviceAccountId)) {
            throw new BusinessException("Device account already exists on this device");
        }
        return deviceAccountRepository.update(deviceAccountId, command);
    }

    public void delete(long deviceAccountId) {
        deviceAccountRepository.delete(deviceAccountId);
    }

    private void validateUpsert(DeviceAccountUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.deviceNodeId() == null) {
            throw new IllegalArgumentException("deviceNodeId is required");
        }
        if (!deviceAccountRepository.existsActiveDeviceNode(command.deviceNodeId())) {
            throw new IllegalArgumentException("deviceNodeId does not exist");
        }
        if (command.userId() != null && !deviceAccountRepository.existsActiveUser(command.userId())) {
            throw new IllegalArgumentException("userId does not exist");
        }
        if (command.accountName() == null || command.accountName().isBlank()) {
            throw new IllegalArgumentException("accountName is required");
        }
        validateMaxLength("accountName", command.accountName(), 100);
        if (command.accountStatus() == null || command.accountStatus().isBlank()) {
            throw new IllegalArgumentException("accountStatus is required");
        }
        validateAccountStatus(command.accountStatus());
        if (command.sourceType() == null || command.sourceType().isBlank()) {
            throw new IllegalArgumentException("sourceType is required");
        }
        validateMaxLength("sourceType", command.sourceType(), 30);
        if (command.remark() != null) {
            validateMaxLength("remark", command.remark(), 255);
        }
    }

    private void validateAccountStatus(String accountStatus) {
        if (!ACCOUNT_STATUS_ENABLED.equals(accountStatus) && !ACCOUNT_STATUS_DISABLED.equals(accountStatus)) {
            throw new IllegalArgumentException("accountStatus must be ENABLED or DISABLED");
        }
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
    }
}
