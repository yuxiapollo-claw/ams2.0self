package com.company.ams.user;

import com.company.ams.common.api.ApiResponse;
import com.company.ams.common.api.ListPayload;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<ListPayload<UserRow>> list() {
        List<UserRow> users = userService.list();
        return ApiResponse.success(new ListPayload<>(users, users.size()));
    }

    @PostMapping
    public ApiResponse<UserRow> create(@RequestBody UserUpsertCommand command) {
        return ApiResponse.success(userService.create(command));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserRow> update(
            @PathVariable long userId,
            @RequestBody UserUpsertCommand command) {
        return ApiResponse.success(userService.update(userId, command));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserRow> patchStatus(
            @PathVariable long userId,
            @RequestBody UserStatusCommand command) {
        return ApiResponse.success(userService.updateStatus(userId, command));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable long userId) {
        userService.delete(userId);
        return ApiResponse.success(null);
    }
}
