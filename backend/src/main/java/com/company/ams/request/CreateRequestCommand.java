package com.company.ams.request;

import java.util.List;

public record CreateRequestCommand(
        String requestType,
        Long targetUserId,
        Long targetDeviceNodeId,
        String targetAccountName,
        String reason,
        List<RequestItemCommand> items) {}
