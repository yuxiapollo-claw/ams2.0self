package com.company.ams.auth;

import java.io.Serializable;
import java.util.List;

public record UserPrincipal(
        Long id,
        String loginName,
        String userName,
        boolean systemAdmin,
        List<String> roles) implements Serializable {
    public UserPrincipal(Long id, String loginName, String userName, List<String> roles) {
        this(
                id,
                loginName,
                userName,
                roles != null && roles.contains("SYS_ADMIN"),
                roles == null ? List.of() : List.copyOf(roles));
    }
}
