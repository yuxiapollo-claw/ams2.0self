package com.company.ams.access;

public record PermissionNodeUpsertCommand(
        Long systemId,
        Long parentPermissionId,
        String permissionName,
        Boolean enabled) {}
