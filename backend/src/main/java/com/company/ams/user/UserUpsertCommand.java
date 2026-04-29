package com.company.ams.user;

public record UserUpsertCommand(
        String userCode,
        String userName,
        Long departmentId,
        String employmentStatus,
        String loginName,
        String accountStatus) {}
