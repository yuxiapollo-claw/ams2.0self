package com.company.ams.user;

import java.util.List;

public record DepartmentMemberBindCommand(List<Long> memberUserIds) {}
