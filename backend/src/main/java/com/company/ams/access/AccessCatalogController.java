package com.company.ams.access;

import com.company.ams.auth.AuthenticatedUserResolver;
import com.company.ams.auth.UserPrincipal;
import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
public class AccessCatalogController {
    private final AccessManagementService accessManagementService;

    public AccessCatalogController(AccessManagementService accessManagementService) {
        this.accessManagementService = accessManagementService;
    }

    @GetMapping("/systems")
    public ApiResponse<ListPayload<AccessSystemRow>> listSystems(Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        List<AccessSystemRow> systems = accessManagementService.listSystems(principal);
        return ApiResponse.success(new ListPayload<>(systems, systems.size()));
    }

    @PostMapping("/systems")
    public ApiResponse<AccessSystemRow> createSystem(
            @RequestBody AccessSystemUpsertCommand command,
            Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        return ApiResponse.success(accessManagementService.createSystem(command, principal));
    }

    @PutMapping("/systems/{systemId}")
    public ApiResponse<AccessSystemRow> updateSystem(
            @PathVariable long systemId,
            @RequestBody AccessSystemUpsertCommand command,
            Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        return ApiResponse.success(accessManagementService.updateSystem(systemId, command, principal));
    }

    @DeleteMapping("/systems/{systemId}")
    public ApiResponse<Void> deleteSystem(@PathVariable long systemId, Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        accessManagementService.deleteSystem(systemId, principal);
        return ApiResponse.success(null);
    }

    @GetMapping("/permissions/tree")
    public ApiResponse<ListPayload<PermissionTreeNode>> listPermissionTree(Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        List<PermissionTreeNode> tree = accessManagementService.listPermissionTree(principal);
        return ApiResponse.success(new ListPayload<>(tree, tree.size()));
    }

    @GetMapping("/permissions/my-tree")
    public ApiResponse<ListPayload<PermissionTreeNode>> listMyPermissionTree(Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        List<PermissionTreeNode> tree = accessManagementService.listMyPermissionTree(principal);
        return ApiResponse.success(new ListPayload<>(tree, tree.size()));
    }

    @PostMapping("/permissions")
    public ApiResponse<PermissionTreeNode> createPermission(
            @RequestBody PermissionNodeUpsertCommand command,
            Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        return ApiResponse.success(accessManagementService.createPermission(command, principal));
    }

    @PutMapping("/permissions/{permissionId}")
    public ApiResponse<PermissionTreeNode> updatePermission(
            @PathVariable long permissionId,
            @RequestBody PermissionNodeUpsertCommand command,
            Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        return ApiResponse.success(accessManagementService.updatePermission(permissionId, command, principal));
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ApiResponse<Void> deletePermission(@PathVariable long permissionId, Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        accessManagementService.deletePermission(permissionId, principal);
        return ApiResponse.success(null);
    }
}
