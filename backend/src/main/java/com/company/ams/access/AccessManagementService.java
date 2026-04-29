package com.company.ams.access;

import com.company.ams.auth.UserPrincipal;
import com.company.ams.common.api.BusinessException;
import com.company.ams.common.persistence.AccessCatalogRepository;
import com.company.ams.common.persistence.AccessCatalogRepository.PermissionRecord;
import com.company.ams.common.persistence.AccessCatalogRepository.SystemRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AccessManagementService {
    private static final int MAX_TREE_LEVEL = 5;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final AccessCatalogRepository accessCatalogRepository;

    public AccessManagementService(AccessCatalogRepository accessCatalogRepository) {
        this.accessCatalogRepository = accessCatalogRepository;
    }

    public List<AccessSystemRow> listSystems(UserPrincipal principal) {
        requireSystemAdmin(principal);
        return accessCatalogRepository.findSystems();
    }

    public AccessSystemRow createSystem(AccessSystemUpsertCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        validateSystem(command);
        if (accessCatalogRepository.existsSystemName(command.systemName().trim(), null)) {
            throw new BusinessException("System name already exists");
        }
        return accessCatalogRepository.createSystem(normalizedSystem(command));
    }

    public AccessSystemRow updateSystem(long systemId, AccessSystemUpsertCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        validateSystem(command);
        if (accessCatalogRepository.existsSystemName(command.systemName().trim(), systemId)) {
            throw new BusinessException("System name already exists");
        }
        SystemRecord existing = accessCatalogRepository.getRequiredSystemRecord(systemId);
        AccessSystemRow updated = accessCatalogRepository.updateSystem(systemId, normalizedSystem(command));
        if (!existing.systemName().equals(updated.systemName())) {
            for (PermissionRecord permission : accessCatalogRepository.findPermissions()) {
                if (permission.systemId() == systemId) {
                    accessCatalogRepository.updatePermissionPath(
                            permission.id(),
                            updated.systemName() + permission.fullPath().substring(existing.systemName().length()),
                            permission.level());
                }
            }
        }
        return updated;
    }

    public void deleteSystem(long systemId, UserPrincipal principal) {
        requireSystemAdmin(principal);
        if (accessCatalogRepository.hasPermissionsUnderSystem(systemId)) {
            throw new BusinessException("Delete child permissions before removing this system");
        }
        accessCatalogRepository.deleteSystem(systemId);
    }

    public List<PermissionTreeNode> listPermissionTree(UserPrincipal principal) {
        requireAuthenticated(principal);
        return buildTree(null);
    }

    public List<PermissionTreeNode> listMyPermissionTree(UserPrincipal principal) {
        requireAuthenticated(principal);
        Set<Long> assignedIds = Set.copyOf(accessCatalogRepository.findAssignedPermissionIds(principal.id()));
        return buildTree(assignedIds);
    }

    public PermissionTreeNode createPermission(PermissionNodeUpsertCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        PermissionNodeUpsertCommand normalized = normalizedPermission(command);
        PermissionContext context = validatePermissionCreate(normalized);
        if (accessCatalogRepository.existsPermissionName(
                normalized.systemId(),
                normalized.parentPermissionId(),
                normalized.permissionName(),
                null)) {
            throw new BusinessException("Permission name already exists under the current parent");
        }
        PermissionRecord created = accessCatalogRepository.createPermission(
                normalized,
                context.fullPath(),
                context.level());
        return toPermissionNode(created, List.of());
    }

    public PermissionTreeNode updatePermission(long permissionId, PermissionNodeUpsertCommand command, UserPrincipal principal) {
        requireSystemAdmin(principal);
        PermissionRecord existing = accessCatalogRepository.getRequiredPermissionRecord(permissionId);
        PermissionNodeUpsertCommand normalized = normalizedPermission(command);
        validatePermissionUpdate(existing, normalized);
        PermissionContext context = buildPermissionContext(existing.systemId(), existing.parentPermissionId(), normalized.permissionName());
        if (accessCatalogRepository.existsPermissionName(
                existing.systemId(),
                existing.parentPermissionId(),
                normalized.permissionName(),
                permissionId)) {
            throw new BusinessException("Permission name already exists under the current parent");
        }
        String oldPrefix = existing.fullPath();
        PermissionRecord updated = accessCatalogRepository.updatePermission(
                permissionId,
                new PermissionNodeUpsertCommand(
                        existing.systemId(),
                        existing.parentPermissionId(),
                        normalized.permissionName(),
                        normalized.enabled()),
                context.fullPath(),
                context.level());
        updateDescendantPaths(permissionId, oldPrefix, context.fullPath(), context.level());
        return toPermissionNode(updated, List.of());
    }

    public void deletePermission(long permissionId, UserPrincipal principal) {
        requireSystemAdmin(principal);
        if (accessCatalogRepository.hasChildPermissions(permissionId)) {
            throw new BusinessException("Delete child permissions before removing this permission");
        }
        if (accessCatalogRepository.hasAssignments(permissionId)) {
            throw new BusinessException("Permission is already assigned to users");
        }
        if (accessCatalogRepository.hasRequests(permissionId)) {
            throw new BusinessException("Permission has related requests and cannot be deleted");
        }
        accessCatalogRepository.deletePermission(permissionId);
    }

    private List<PermissionTreeNode> buildTree(Set<Long> includedPermissionIds) {
        List<AccessSystemRow> systems = accessCatalogRepository.findSystems();
        List<PermissionRecord> permissions = accessCatalogRepository.findPermissions();

        Map<Long, List<PermissionRecord>> childrenByParent = new LinkedHashMap<>();
        Map<Long, List<PermissionRecord>> rootsBySystem = new LinkedHashMap<>();
        for (PermissionRecord permission : permissions) {
            if (permission.parentPermissionId() == null) {
                rootsBySystem.computeIfAbsent(permission.systemId(), ignored -> new ArrayList<>()).add(permission);
            } else {
                childrenByParent.computeIfAbsent(permission.parentPermissionId(), ignored -> new ArrayList<>()).add(permission);
            }
        }

        List<PermissionTreeNode> result = new ArrayList<>();
        for (AccessSystemRow system : systems) {
            List<PermissionTreeNode> children = new ArrayList<>();
            for (PermissionRecord rootPermission : rootsBySystem.getOrDefault(system.id(), List.of())) {
                PermissionTreeNode childNode = buildPermissionSubTree(rootPermission, childrenByParent, includedPermissionIds);
                if (childNode != null) {
                    children.add(childNode);
                }
            }
            if (includedPermissionIds != null && children.isEmpty()) {
                continue;
            }
            result.add(new PermissionTreeNode(
                    "SYSTEM-" + system.id(),
                    system.id(),
                    "SYSTEM",
                    system.id(),
                    null,
                    system.systemName(),
                    system.systemName(),
                    true,
                    1,
                    children.isEmpty(),
                    children));
        }
        return result;
    }

    private PermissionTreeNode buildPermissionSubTree(
            PermissionRecord permission,
            Map<Long, List<PermissionRecord>> childrenByParent,
            Set<Long> includedPermissionIds) {
        List<PermissionTreeNode> children = new ArrayList<>();
        for (PermissionRecord child : childrenByParent.getOrDefault(permission.id(), List.of())) {
            PermissionTreeNode childNode = buildPermissionSubTree(child, childrenByParent, includedPermissionIds);
            if (childNode != null) {
                children.add(childNode);
            }
        }
        boolean included = includedPermissionIds == null || includedPermissionIds.contains(permission.id()) || !children.isEmpty();
        if (!included) {
            return null;
        }
        return toPermissionNode(permission, children);
    }

    private PermissionTreeNode toPermissionNode(PermissionRecord permission, List<PermissionTreeNode> children) {
        return new PermissionTreeNode(
                "PERMISSION-" + permission.id(),
                permission.id(),
                "PERMISSION",
                permission.systemId(),
                permission.parentPermissionId(),
                permission.permissionName(),
                permission.fullPath(),
                permission.enabled(),
                permission.level(),
                children.isEmpty(),
                children);
    }

    private PermissionNodeUpsertCommand normalizedPermission(PermissionNodeUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        return new PermissionNodeUpsertCommand(
                command.systemId(),
                command.parentPermissionId(),
                command.permissionName() == null ? null : command.permissionName().trim(),
                command.enabled());
    }

    private AccessSystemUpsertCommand normalizedSystem(AccessSystemUpsertCommand command) {
        return new AccessSystemUpsertCommand(
                command.systemName().trim(),
                command.systemDescription() == null ? null : command.systemDescription().trim());
    }

    private PermissionContext validatePermissionCreate(PermissionNodeUpsertCommand command) {
        validatePermissionPayload(command);
        return buildPermissionContext(command.systemId(), command.parentPermissionId(), command.permissionName());
    }

    private void validatePermissionUpdate(PermissionRecord existing, PermissionNodeUpsertCommand command) {
        validatePermissionPayload(command);
        if (command.systemId() == null || existing.systemId() != command.systemId()) {
            throw new IllegalArgumentException("systemId cannot be changed");
        }
        if ((existing.parentPermissionId() == null && command.parentPermissionId() != null)
                || (existing.parentPermissionId() != null && !existing.parentPermissionId().equals(command.parentPermissionId()))) {
            throw new IllegalArgumentException("parentPermissionId cannot be changed");
        }
    }

    private void validatePermissionPayload(PermissionNodeUpsertCommand command) {
        if (command.systemId() == null) {
            throw new IllegalArgumentException("systemId is required");
        }
        if (command.permissionName() == null || command.permissionName().isBlank()) {
            throw new IllegalArgumentException("permissionName is required");
        }
        if (command.permissionName().length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("permissionName must be at most 100 characters");
        }
        if (command.enabled() == null) {
            throw new IllegalArgumentException("enabled is required");
        }
    }

    private PermissionContext buildPermissionContext(long systemId, Long parentPermissionId, String permissionName) {
        SystemRecord system = accessCatalogRepository.getRequiredSystemRecord(systemId);
        if (parentPermissionId == null) {
            int level = 2;
            if (level > MAX_TREE_LEVEL) {
                throw new IllegalArgumentException("Permission level exceeds the maximum depth of 5");
            }
            return new PermissionContext(level, system.systemName() + "/" + permissionName);
        }
        PermissionRecord parent = accessCatalogRepository.getRequiredPermissionRecord(parentPermissionId);
        if (parent.systemId() != systemId) {
            throw new IllegalArgumentException("parentPermissionId does not belong to the specified system");
        }
        int level = parent.level() + 1;
        if (level > MAX_TREE_LEVEL) {
            throw new IllegalArgumentException("Permission level exceeds the maximum depth of 5");
        }
        return new PermissionContext(level, parent.fullPath() + "/" + permissionName);
    }

    private void updateDescendantPaths(long permissionId, String oldPrefix, String newPrefix, int newLevel) {
        if (oldPrefix.equals(newPrefix)) {
            return;
        }
        List<PermissionRecord> permissions = accessCatalogRepository.findPermissions();
        for (PermissionRecord permission : permissions) {
            if (permission.id() == permissionId) {
                continue;
            }
            if (permission.fullPath().startsWith(oldPrefix + "/")) {
                String suffix = permission.fullPath().substring(oldPrefix.length());
                int levelDiff = permission.level() - newLevel;
                accessCatalogRepository.updatePermissionPath(
                        permission.id(),
                        newPrefix + suffix,
                        newLevel + levelDiff);
            }
        }
    }

    private void validateSystem(AccessSystemUpsertCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.systemName() == null || command.systemName().isBlank()) {
            throw new IllegalArgumentException("systemName is required");
        }
        if (command.systemName().trim().length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("systemName must be at most 100 characters");
        }
        if (command.systemDescription() != null && command.systemDescription().trim().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("systemDescription must be at most 255 characters");
        }
    }

    private void requireSystemAdmin(UserPrincipal principal) {
        if (principal == null || !principal.systemAdmin()) {
            throw new AccessDeniedException("System administrator access required");
        }
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    private record PermissionContext(int level, String fullPath) {}
}
