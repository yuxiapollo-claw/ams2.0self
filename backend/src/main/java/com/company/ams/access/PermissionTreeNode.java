package com.company.ams.access;

import java.util.List;

public record PermissionTreeNode(
        String key,
        Long entityId,
        String nodeType,
        Long systemId,
        Long parentPermissionId,
        String label,
        String fullPath,
        boolean enabled,
        int level,
        boolean leaf,
        List<PermissionTreeNode> children) {}
