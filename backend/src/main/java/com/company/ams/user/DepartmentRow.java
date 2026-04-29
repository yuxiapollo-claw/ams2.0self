package com.company.ams.user;

public record DepartmentRow(
        Long id,
        String departmentName,
        Long managerUserId,
        String managerUserName,
        String description,
        int memberCount,
        String status,
        String updatedAt) {}
