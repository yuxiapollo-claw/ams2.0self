package com.company.ams.admin;

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
@RequestMapping("/api/admin/application-configs")
public class ApplicationConfigController {
    private final ApplicationConfigService applicationConfigService;

    public ApplicationConfigController(ApplicationConfigService applicationConfigService) {
        this.applicationConfigService = applicationConfigService;
    }

    @GetMapping
    public ApiResponse<ListPayload<ApplicationConfigRow>> list() {
        List<ApplicationConfigRow> rows = applicationConfigService.list();
        return ApiResponse.success(new ListPayload<>(rows, rows.size()));
    }

    @PostMapping
    public ApiResponse<ApplicationConfigRow> create(@RequestBody ApplicationConfigUpsertCommand command) {
        return ApiResponse.success(applicationConfigService.create(command));
    }

    @PutMapping("/{configId}")
    public ApiResponse<ApplicationConfigRow> update(
            @PathVariable long configId,
            @RequestBody ApplicationConfigUpsertCommand command) {
        return ApiResponse.success(applicationConfigService.update(configId, command));
    }

    @DeleteMapping("/{configId}")
    public ApiResponse<Void> delete(@PathVariable long configId) {
        applicationConfigService.delete(configId);
        return ApiResponse.success(null);
    }
}
