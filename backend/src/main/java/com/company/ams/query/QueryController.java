package com.company.ams.query;

import com.company.ams.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queries")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/device-permissions")
    public ApiResponse<DevicePermissionPayload> devicePermissions(@RequestParam Long deviceNodeId) {
        return ApiResponse.success(queryService.devicePermissions(deviceNodeId));
    }
}
