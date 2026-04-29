package com.company.ams.auth;

import java.util.List;

public record LoginResponse(String token, LoginUser user, List<String> roles) {
    public record LoginUser(Long id, String loginName, String userName, boolean systemAdmin) {
    }
}
