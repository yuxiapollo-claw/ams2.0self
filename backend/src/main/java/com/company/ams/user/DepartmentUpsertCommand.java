package com.company.ams.user;

public record DepartmentUpsertCommand(
        String departmentName,
        Long managerUserId,
        String description,
        String status) {}
