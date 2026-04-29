package com.company.ams.account;

import java.util.List;

public record DeviceAccountRow(
        Long id,
        Long deviceNodeId,
        String deviceName,
        Long userId,
        String userName,
        String accountName,
        String accountStatus,
        String sourceType,
        String remark,
        List<String> roles) {}
