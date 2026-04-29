package com.company.ams.auth;

import java.io.Serializable;
import java.util.List;

public record UserPrincipal(Long id, String loginName, String userName, List<String> roles) implements Serializable {
}
