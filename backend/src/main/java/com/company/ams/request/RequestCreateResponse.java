package com.company.ams.request;

import java.util.List;

public record RequestCreateResponse(
        long id,
        String requestNo,
        String currentStatus,
        String currentStatusLabel,
        List<RequestWorkflowItem> items) {}
