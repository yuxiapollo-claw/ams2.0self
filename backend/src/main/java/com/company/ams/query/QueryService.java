package com.company.ams.query;

import com.company.ams.common.persistence.DeviceAccountRepository;
import com.company.ams.common.persistence.DeviceAccountRepository.DevicePermissionEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
    private final DeviceAccountRepository deviceAccountRepository;

    public QueryService(DeviceAccountRepository deviceAccountRepository) {
        this.deviceAccountRepository = deviceAccountRepository;
    }

    public DevicePermissionPayload devicePermissions(Long deviceNodeId) {
        Map<String, List<DevicePermissionAccount>> accountsByRole = new LinkedHashMap<>();
        for (DevicePermissionEntry entry : deviceAccountRepository.findPermissionEntries(deviceNodeId)) {
            accountsByRole.computeIfAbsent(entry.roleName(), ignored -> new ArrayList<>())
                    .add(new DevicePermissionAccount(entry.userName(), entry.accountName()));
        }
        List<DevicePermissionRole> roles = accountsByRole.entrySet().stream()
                .map(entry -> new DevicePermissionRole(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        return new DevicePermissionPayload(deviceNodeId, roles);
    }
}

record DevicePermissionPayload(Long deviceNodeId, List<DevicePermissionRole> roles) {}

record DevicePermissionRole(String roleName, List<DevicePermissionAccount> accounts) {}

record DevicePermissionAccount(String userName, String accountName) {}
