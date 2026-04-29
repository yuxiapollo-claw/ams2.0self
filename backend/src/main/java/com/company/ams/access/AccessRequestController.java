package com.company.ams.access;

import com.company.ams.auth.AuthenticatedUserResolver;
import com.company.ams.auth.UserPrincipal;
import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access/requests")
public class AccessRequestController {
    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @GetMapping
    public ApiResponse<ListPayload<PermissionRequestRow>> list(Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        List<PermissionRequestRow> requests = accessRequestService.list(principal);
        return ApiResponse.success(new ListPayload<>(requests, requests.size()));
    }

    @PostMapping
    public ApiResponse<PermissionRequestRow> create(
            @RequestBody PermissionRequestCommand command,
            Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        return ApiResponse.success(accessRequestService.create(command, principal));
    }
}
