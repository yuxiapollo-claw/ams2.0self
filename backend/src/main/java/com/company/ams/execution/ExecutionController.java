package com.company.ams.execution;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.auth.AuthenticatedUserResolver;
import com.company.ams.auth.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {
    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{requestId}/submit")
    public ApiResponse<ExecutionSubmitResponse> submit(
            @PathVariable Long requestId,
            Authentication authentication) {
        UserPrincipal principal = AuthenticatedUserResolver.resolve(authentication);
        return ApiResponse.success(executionService.submit(requestId, principal.userName()));
    }
}
