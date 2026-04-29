package com.company.ams.request;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.auth.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    public ApiResponse<RequestListPayload> list() {
        return ApiResponse.success(requestService.list());
    }

    @PostMapping
    public ApiResponse<RequestCreateResponse> create(
            @RequestBody CreateRequestCommand command,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(requestService.create(command, principal));
    }

    @PostMapping("/{requestId}/approve")
    public ApiResponse<RequestAdvanceResponse> approve(@PathVariable long requestId) {
        return ApiResponse.success(requestService.advance(requestId, "APPROVE"));
    }
}
