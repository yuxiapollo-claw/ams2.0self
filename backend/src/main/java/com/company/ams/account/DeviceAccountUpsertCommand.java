package com.company.ams.account;

public record DeviceAccountUpsertCommand(
        Long deviceNodeId,
        Long userId,
        String accountName,
        String accountStatus,
        String sourceType,
        String remark) {}
