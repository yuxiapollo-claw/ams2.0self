package com.company.ams.user;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import java.util.List;
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
    public ApiResponse<ListPayload<DepartmentRow>> list() {
        List<DepartmentRow> departments = departmentService.list();
        return ApiResponse.success(new ListPayload<>(departments, departments.size()));
    }

    @PostMapping
    public ApiResponse<DepartmentRow> create(@RequestBody DepartmentUpsertCommand command) {
        return ApiResponse.success(departmentService.create(command));
    }

    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentRow> update(
            @PathVariable long departmentId,
            @RequestBody DepartmentUpsertCommand command) {
        return ApiResponse.success(departmentService.update(departmentId, command));
    }

    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> delete(@PathVariable long departmentId) {
        departmentService.delete(departmentId);
        return ApiResponse.success(null);
    }
}
