package com.company.ams.user;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import com.company.ams.auth.AuthenticatedUserResolver;
import com.company.ams.auth.UserPrincipal;
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
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ApiResponse<ListPayload<DepartmentRow>> list(Authentication authentication) {
        List<DepartmentRow> departments = AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? departmentService.list(principal(authentication))
                : departmentService.list();
        return ApiResponse.success(new ListPayload<>(departments, departments.size()));
    }

    @PostMapping
    public ApiResponse<DepartmentRow> create(
            @RequestBody DepartmentUpsertCommand command,
            Authentication authentication) {
        return ApiResponse.success(AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? departmentService.create(command, principal(authentication))
                : departmentService.create(command));
    }

    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentRow> update(
            @PathVariable long departmentId,
            @RequestBody DepartmentUpsertCommand command,
            Authentication authentication) {
        return ApiResponse.success(AuthenticatedUserResolver.hasNativePrincipal(authentication)
                ? departmentService.update(departmentId, command, principal(authentication))
                : departmentService.update(departmentId, command));
    }

    @GetMapping("/{departmentId}/members")
    public ApiResponse<ListPayload<DepartmentMemberRow>> listMembers(
            @PathVariable long departmentId,
            Authentication authentication) {
        List<DepartmentMemberRow> members = departmentService.listMembers(departmentId, principal(authentication));
        return ApiResponse.success(new ListPayload<>(members, members.size()));
    }

    @PutMapping("/{departmentId}/members")
    public ApiResponse<ListPayload<DepartmentMemberRow>> addMembers(
            @PathVariable long departmentId,
            @RequestBody DepartmentMemberBindCommand command,
            Authentication authentication) {
        List<DepartmentMemberRow> members = departmentService.addMembers(
                departmentId,
                command,
                principal(authentication));
        return ApiResponse.success(new ListPayload<>(members, members.size()));
    }

    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> delete(@PathVariable long departmentId, Authentication authentication) {
        if (AuthenticatedUserResolver.hasNativePrincipal(authentication)) {
            departmentService.delete(departmentId, principal(authentication));
        } else {
            departmentService.delete(departmentId);
        }
        return ApiResponse.success(null);
    }

    private UserPrincipal principal(Authentication authentication) {
        return AuthenticatedUserResolver.resolve(authentication);
    }
}
