package com.company.ams.common.api;

import java.util.List;

public record ListPayload<T>(List<T> list, int total) {}

