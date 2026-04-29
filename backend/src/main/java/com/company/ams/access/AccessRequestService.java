package com.company.ams.access;

import com.company.ams.auth.UserPrincipal;
import com.company.ams.common.api.BusinessException;
import com.company.ams.common.persistence.AccessCatalogRepository;
import com.company.ams.common.persistence.AccessCatalogRepository.PermissionRecord;
import com.company.ams.common.persistence.AccessRequestRepository;
import com.company.ams.common.persistence.AccessRequestRepository.DepartmentLeaderSnapshot;
import com.company.ams.common.persistence.AuditRepository;
import com.company.ams.common.persistence.UserRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AccessRequestService {
    public static final String REQUEST_TYPE_APPLY = "PERMISSION_APPLY";
    public static final String REQUEST_TYPE_REMOVE = "PERMISSION_REMOVE";
    public static final String REQUEST_TYPE_RESET = "PASSWORD_RESET";

    private final AccessCatalogRepository accessCatalogRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    public AccessRequestService(
            AccessCatalogRepository accessCatalogRepository,
            AccessRequestRepository accessRequestRepository,
            AuditRepository auditRepository,
            UserRepository userRepository) {
        this.accessCatalogRepository = accessCatalogRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    public PermissionRequestRow create(PermissionRequestCommand command, UserPrincipal principal) {
        requireAuthenticated(principal);
        validateCommand(command);
        validateRequestType(command.requestType());

        UserRepository.RequestUserRecord applicant = userRepository.findRequestUserById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("Applicant " + principal.id() + " does not exist"));
        PermissionRecord permission = accessCatalogRepository.getRequiredPermissionRecord(command.permissionId());
        if (!permission.enabled()) {
            throw new BusinessException("Permission is disabled");
        }
        if (accessCatalogRepository.hasChildPermissions(permission.id())) {
            throw new BusinessException("Select a final permission path instead of a parent node");
        }

        boolean alreadyAssigned = accessCatalogRepository.findAssignedPermissionIds(principal.id())
                .contains(permission.id());
        switch (command.requestType()) {
            case REQUEST_TYPE_APPLY -> {
                if (alreadyAssigned) {
                    throw new BusinessException("Current user already owns this permission");
                }
            }
            case REQUEST_TYPE_REMOVE, REQUEST_TYPE_RESET -> {
                if (!alreadyAssigned) {
                    throw new BusinessException("Current user does not own this permission");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported requestType");
        }

        if (accessRequestRepository.existsOpenRequest(principal.id(), permission.id(), command.requestType())) {
            throw new BusinessException("An unfinished request for this permission already exists");
        }

        DepartmentLeaderSnapshot leaderSnapshot = accessRequestRepository.findDepartmentLeaderSnapshot(applicant.departmentId());
        PermissionRequestRow created = accessRequestRepository.createRequest(
                applicant.id(),
                applicant.departmentId(),
                principal.id(),
                permission.id(),
                permission.fullPath(),
                command.requestType(),
                command.reason().trim(),
                "PENDING",
                "DEPARTMENT_LEADER_REVIEW",
                leaderSnapshot.leaderUserId(),
                leaderSnapshot.leaderUserName());
        auditRepository.insert("ACCESS_REQUEST_CREATED", principal.userName(), "ACCESS_REQUEST", created.id());
        return created;
    }

    public List<PermissionRequestRow> list(UserPrincipal principal) {
        requireAuthenticated(principal);
        return accessRequestRepository.listRequests(principal.id(), principal.systemAdmin());
    }

    private void validateCommand(PermissionRequestCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.permissionId() == null) {
            throw new IllegalArgumentException("permissionId is required");
        }
        if (command.requestType() == null || command.requestType().isBlank()) {
            throw new IllegalArgumentException("requestType is required");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (command.reason().trim().length() > 255) {
            throw new IllegalArgumentException("reason must be at most 255 characters");
        }
    }

    private void validateRequestType(String requestType) {
        if (!REQUEST_TYPE_APPLY.equals(requestType)
                && !REQUEST_TYPE_REMOVE.equals(requestType)
                && !REQUEST_TYPE_RESET.equals(requestType)) {
            throw new IllegalArgumentException("requestType is invalid");
        }
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }
}
