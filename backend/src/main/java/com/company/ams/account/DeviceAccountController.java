package com.company.ams.account;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-accounts")
public class DeviceAccountController {
    private final DeviceAccountService deviceAccountService;

    public DeviceAccountController(DeviceAccountService deviceAccountService) {
        this.deviceAccountService = deviceAccountService;
    }

    @GetMapping
    public ApiResponse<ListPayload<DeviceAccountRow>> list() {
        List<DeviceAccountRow> deviceAccounts = deviceAccountService.list();
        return ApiResponse.success(new ListPayload<>(deviceAccounts, deviceAccounts.size()));
    }

    @GetMapping("/by-device")
    public ApiResponse<ListPayload<DeviceAccountRow>> byDevice(@RequestParam Long deviceNodeId) {
        List<DeviceAccountRow> deviceAccounts = deviceAccountService.byDevice(deviceNodeId);
        return ApiResponse.success(new ListPayload<>(deviceAccounts, deviceAccounts.size()));
    }

    @PostMapping
    public ApiResponse<DeviceAccountRow> create(@RequestBody DeviceAccountUpsertCommand command) {
        return ApiResponse.success(deviceAccountService.create(command));
    }

    @PutMapping("/{deviceAccountId}")
    public ApiResponse<DeviceAccountRow> update(
            @PathVariable long deviceAccountId,
            @RequestBody DeviceAccountUpsertCommand command) {
        return ApiResponse.success(deviceAccountService.update(deviceAccountId, command));
    }

    @DeleteMapping("/{deviceAccountId}")
    public ApiResponse<Void> delete(@PathVariable long deviceAccountId) {
        deviceAccountService.delete(deviceAccountId);
        return ApiResponse.success(null);
    }
}
