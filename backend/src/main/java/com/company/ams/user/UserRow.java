package com.company.ams.user;

public record UserRow(
        Long id,
        String userCode,
        String userName,
        Long departmentId,
        String departmentName,
        String employmentStatus,
        String loginName,
        String accountStatus) {}
