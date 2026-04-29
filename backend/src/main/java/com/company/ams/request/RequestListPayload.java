package com.company.ams.request;

import java.util.List;

public record RequestListPayload(
        List<RequestListItem> list,
        int total) {}
