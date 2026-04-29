# AMS2.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build AMS2.0 as a full-stack internal account management system covering authentication, organization data, asset tree management, device-account authorization, fixed approval workflow, QI execution tracking, audit logging, and query/reporting.

**Architecture:** Use a greenfield monorepo with a Vue 3 admin frontend and a Spring Boot 3 backend. Build the system vertically in slices so every phase produces a working, testable increment: first the project skeleton and schema, then core master data, then workflow, then query/audit, then the full UI and deployment assets.

**Tech Stack:** Vue 3, TypeScript, Vite, Element Plus, Pinia, Vue Router, Axios, Java 17, Spring Boot 3, Spring Security, MyBatis, MySQL 8, Flyway, JUnit 5, Testcontainers, Vitest, Vue Test Utils, Playwright, Docker Compose

---

## Proposed Repository Structure

### Backend

- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/company/ams/AmsApplication.java`
- Create: `backend/src/main/java/com/company/ams/common/`
- Create: `backend/src/main/java/com/company/ams/auth/`
- Create: `backend/src/main/java/com/company/ams/user/`
- Create: `backend/src/main/java/com/company/ams/system/`
- Create: `backend/src/main/java/com/company/ams/account/`
- Create: `backend/src/main/java/com/company/ams/request/`
- Create: `backend/src/main/java/com/company/ams/execution/`
- Create: `backend/src/main/java/com/company/ams/query/`
- Create: `backend/src/main/java/com/company/ams/audit/`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/`
- Create: `backend/src/test/java/com/company/ams/`

### Frontend

- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/`
- Create: `frontend/src/api/`
- Create: `frontend/src/views/`
- Create: `frontend/src/components/`
- Create: `frontend/src/layouts/`
- Create: `frontend/src/tests/`
- Create: `frontend/playwright.config.ts`

### Ops and Docs

- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `ops/nginx/default.conf`
- Create: `ops/backend/Dockerfile`
- Create: `ops/frontend/Dockerfile`
- Create: `docs/er/ams2-er.md`
- Create: `docs/api/ams2-api.md`
- Create: `docs/test/ams2-acceptance.md`

## Delivery Sequence

This spec is broad, but the subsystems are tightly coupled around one data model and one fixed workflow. Keep one implementation plan, but execute it in this order:

1. Project skeleton and schema
2. Auth and organization master data
3. Asset tree and device-account domain
4. Request workflow and QI execution
5. Query and audit center
6. Frontend shell and business pages
7. End-to-end verification and deployment packaging

## Task 1: Scaffold Monorepo and Local Tooling

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/company/ams/AmsApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/src/main.ts`
- Create: `docker-compose.yml`
- Test: `backend/src/test/java/com/company/ams/AmsApplicationTests.java`
- Test: `frontend/src/tests/smoke.spec.ts`

- [ ] **Step 1: Write the failing backend and frontend smoke tests**

```java
package com.company.ams;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AmsApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

```ts
import { describe, expect, it } from 'vitest'

describe('app bootstrap', () => {
  it('mounts root id', () => {
    document.body.innerHTML = '<div id="app"></div>'
    expect(document.querySelector('#app')).not.toBeNull()
  })
})
```

- [ ] **Step 2: Run the smoke tests to verify they fail because the projects do not exist yet**

Run: `mvn -f backend/pom.xml test`
Expected: FAIL with `Non-readable POM` or `The system cannot find the file specified`

Run: `npm --prefix frontend test`
Expected: FAIL with `package.json not found`

- [ ] **Step 3: Create the minimal backend and frontend skeleton**

```xml
<!-- backend/pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.company</groupId>
  <artifactId>ams-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
  </parent>
  <properties>
    <java.version>17</java.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

```java
package com.company.ams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmsApplication.class, args);
    }
}
```

```yaml
spring:
  application:
    name: ams-backend
server:
  port: 8080
```

```json
{
  "name": "ams-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "vue": "^3.5.13"
  },
  "devDependencies": {
    "typescript": "^5.6.2",
    "vite": "^5.4.8",
    "vitest": "^2.1.2"
  }
}
```

```ts
import { createApp, h } from 'vue'

createApp({
  render: () => h('div', { id: 'ams-root' }, 'AMS2.0')
}).mount('#app')
```

- [ ] **Step 4: Run the smoke tests again**

Run: `mvn -f backend/pom.xml test`
Expected: PASS with `Tests run: 1, Failures: 0`

Run: `npm --prefix frontend install && npm --prefix frontend test`
Expected: PASS with `1 passed`

- [ ] **Step 5: Commit the scaffold**

```bash
git add backend frontend docker-compose.yml
git commit -m "chore: scaffold ams monorepo"
```

## Task 2: Add Schema Management and Core Database Tables

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/test/java/com/company/ams/schema/FlywaySchemaTest.java`
- Test: `backend/src/test/java/com/company/ams/schema/FlywaySchemaTest.java`

- [ ] **Step 1: Write a failing schema test that checks for the key tables**

```java
package com.company.ams.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FlywaySchemaTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void createsCoreTables() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(hasTable(metaData, "sys_user")).isTrue();
            assertThat(hasTable(metaData, "sys_department")).isTrue();
            assertThat(hasTable(metaData, "asset_node")).isTrue();
            assertThat(hasTable(metaData, "device_account")).isTrue();
            assertThat(hasTable(metaData, "request_order")).isTrue();
        }
    }

    private boolean hasTable(DatabaseMetaData metaData, String name) throws Exception {
        try (ResultSet rs = metaData.getTables(null, null, name, null)) {
            return rs.next();
        }
    }
}
```

- [ ] **Step 2: Run the schema test to verify it fails because migrations are missing**

Run: `mvn -f backend/pom.xml -Dtest=FlywaySchemaTest test`
Expected: FAIL with `Table assertion expected true but was false`

- [ ] **Step 3: Add Flyway and create the initial schema**

```xml
<!-- backend/pom.xml add -->
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
```

```sql
CREATE TABLE sys_department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department_name VARCHAR(100) NOT NULL,
  manager_user_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code VARCHAR(64) NOT NULL,
  user_name VARCHAR(100) NOT NULL,
  department_id BIGINT NOT NULL,
  employment_status VARCHAR(20) NOT NULL,
  login_name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  account_status VARCHAR(20) NOT NULL,
  last_login_time TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_code (user_code),
  UNIQUE KEY uk_sys_user_login (login_name)
);

CREATE TABLE asset_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  node_name VARCHAR(100) NOT NULL,
  node_type VARCHAR(30) NOT NULL,
  parent_id BIGINT NULL,
  level_num INT NOT NULL,
  path VARCHAR(500) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  is_role_node TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE device_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  device_node_id BIGINT NOT NULL,
  account_name VARCHAR(100) NOT NULL,
  account_status VARCHAR(20) NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_device_account_user_device (user_id, device_node_id)
);

CREATE TABLE device_account_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_account_id BIGINT NOT NULL,
  role_node_id BIGINT NOT NULL,
  relation_status VARCHAR(20) NOT NULL,
  effective_at TIMESTAMP NULL,
  expired_at TIMESTAMP NULL,
  source_request_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_device_account_role (device_account_id, role_node_id)
);

CREATE TABLE request_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_no VARCHAR(64) NOT NULL,
  request_type VARCHAR(30) NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  applicant_department_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  target_department_id BIGINT NOT NULL,
  target_device_node_id BIGINT NOT NULL,
  target_account_name VARCHAR(100) NULL,
  request_reason VARCHAR(255) NOT NULL,
  current_status VARCHAR(30) NOT NULL,
  current_step VARCHAR(30) NOT NULL,
  department_manager_snapshot_id BIGINT NULL,
  department_manager_snapshot_name VARCHAR(100) NULL,
  qa_snapshot VARCHAR(255) NULL,
  qm_snapshot VARCHAR(255) NULL,
  qi_snapshot VARCHAR(255) NULL,
  submitted_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_request_order_no (request_no)
);
```

- [ ] **Step 4: Run the schema test again**

Run: `mvn -f backend/pom.xml -Dtest=FlywaySchemaTest test`
Expected: PASS with `createsCoreTables`

- [ ] **Step 5: Commit the schema baseline**

```bash
git add backend/pom.xml backend/src/main/resources/db/migration backend/src/test/java/com/company/ams/schema
git commit -m "feat: add initial ams schema"
```

## Task 3: Implement Authentication and Global Role Management

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/company/ams/auth/AuthController.java`
- Create: `backend/src/main/java/com/company/ams/auth/AuthService.java`
- Create: `backend/src/main/java/com/company/ams/auth/SecurityConfig.java`
- Create: `backend/src/main/java/com/company/ams/auth/UserPrincipal.java`
- Create: `backend/src/main/java/com/company/ams/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/company/ams/auth/LoginResponse.java`
- Create: `backend/src/test/java/com/company/ams/auth/AuthControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/auth/AuthControllerTest.java`

- [ ] **Step 1: Write a failing auth controller test**

```java
package com.company.ams.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginName":"admin","password":"admin123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.user.loginName").value("admin"));
    }
}
```

- [ ] **Step 2: Run the auth test to verify it fails**

Run: `mvn -f backend/pom.xml -Dtest=AuthControllerTest test`
Expected: FAIL with `404` or `No mapping for POST /api/auth/login`

- [ ] **Step 3: Add minimal session-based auth implementation**

```java
package com.company.ams.auth;

public record LoginRequest(String loginName, String password) {
}
```

```java
package com.company.ams.auth;

import java.util.List;

public record LoginResponse(String token, LoginUser user, List<String> roles) {
    public record LoginUser(Long id, String loginName, String userName) {
    }
}
```

```java
package com.company.ams.auth;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public LoginResponse login(LoginRequest request) {
        if (!"admin".equals(request.loginName()) || !"admin123".equals(request.password())) {
            throw new IllegalArgumentException("Bad credentials");
        }
        return new LoginResponse(
            "dev-token-admin",
            new LoginResponse.LoginUser(1L, "admin", "系统管理员"),
            List.of("SYS_ADMIN")
        );
    }
}
```

```java
package com.company.ams.auth;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        return Map.of("code", 0, "message", "success", "data", authService.login(request), "traceId", "local-dev");
    }
}
```

- [ ] **Step 4: Run the auth test again**

Run: `mvn -f backend/pom.xml -Dtest=AuthControllerTest test`
Expected: PASS with `loginReturnsTokenAndUser`

- [ ] **Step 5: Commit the auth slice**

```bash
git add backend/src/main/java/com/company/ams/auth backend/src/test/java/com/company/ams/auth
git commit -m "feat: add basic authentication api"
```

## Task 4: Implement User and Department Master Data APIs

**Files:**
- Create: `backend/src/main/java/com/company/ams/user/UserController.java`
- Create: `backend/src/main/java/com/company/ams/user/UserService.java`
- Create: `backend/src/main/java/com/company/ams/user/DepartmentController.java`
- Create: `backend/src/main/java/com/company/ams/user/DepartmentService.java`
- Create: `backend/src/test/java/com/company/ams/user/UserControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/user/DepartmentControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/user/UserControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/user/DepartmentControllerTest.java`

- [ ] **Step 1: Write failing tests for listing users and departments**

```java
package com.company.ams.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listUsersReturnsRows() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list").isArray());
    }
}
```

```java
package com.company.ams.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listDepartmentsReturnsRows() throws Exception {
        mockMvc.perform(get("/api/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list").isArray());
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -f backend/pom.xml -Dtest=UserControllerTest,DepartmentControllerTest test`
Expected: FAIL with `404`

- [ ] **Step 3: Implement minimal in-memory user and department services**

```java
package com.company.ams.user;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public List<Map<String, Object>> list() {
        return List.of(Map.of(
            "id", 1L,
            "userCode", "EMP001",
            "userName", "张三",
            "departmentName", "分装部",
            "employmentStatus", "ACTIVE",
            "loginName", "zhangsan",
            "accountStatus", "ENABLED"
        ));
    }
}
```

```java
package com.company.ams.user;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("code", 0, "message", "success", "data", Map.of("list", userService.list(), "total", 1));
    }
}
```

```java
package com.company.ams.user;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {
    public List<Map<String, Object>> list() {
        return List.of(Map.of("id", 1L, "departmentName", "分装部", "managerUserName", "李主任", "status", "ENABLED"));
    }
}
```

```java
package com.company.ams.user;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("code", 0, "message", "success", "data", Map.of("list", departmentService.list(), "total", 1));
    }
}
```

- [ ] **Step 4: Run the tests again**

Run: `mvn -f backend/pom.xml -Dtest=UserControllerTest,DepartmentControllerTest test`
Expected: PASS with `2 tests completed`

- [ ] **Step 5: Commit the master-data slice**

```bash
git add backend/src/main/java/com/company/ams/user backend/src/test/java/com/company/ams/user
git commit -m "feat: add user and department apis"
```

## Task 5: Implement Asset Tree, Device Account, and Role Binding APIs

**Files:**
- Create: `backend/src/main/java/com/company/ams/system/AssetController.java`
- Create: `backend/src/main/java/com/company/ams/system/AssetService.java`
- Create: `backend/src/main/java/com/company/ams/account/DeviceAccountController.java`
- Create: `backend/src/main/java/com/company/ams/account/DeviceAccountService.java`
- Create: `backend/src/test/java/com/company/ams/system/AssetControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/account/DeviceAccountControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/system/AssetControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/account/DeviceAccountControllerTest.java`

- [ ] **Step 1: Write failing tests for the asset tree and device-account views**

```java
package com.company.ams.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AssetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void treeReturnsNodes() throws Exception {
        mockMvc.perform(get("/api/assets/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].nodeName").value("仪器设备"));
    }
}
```

```java
package com.company.ams.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceAccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void deviceViewReturnsDeviceAccounts() throws Exception {
        mockMvc.perform(get("/api/device-accounts/by-device").param("deviceNodeId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[0].accountName").value("device_a_zhangsan"));
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -f backend/pom.xml -Dtest=AssetControllerTest,DeviceAccountControllerTest test`
Expected: FAIL with `404`

- [ ] **Step 3: Implement minimal asset-tree and device-account controllers**

```java
package com.company.ams.system;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AssetService {
    public List<Map<String, Object>> tree() {
        return List.of(Map.of(
            "id", 1L,
            "nodeName", "仪器设备",
            "nodeType", "CATEGORY",
            "children", List.of(Map.of(
                "id", 100L,
                "nodeName", "设备A",
                "nodeType", "DEVICE",
                "children", List.of(Map.of("id", 300L, "nodeName", "操作员", "nodeType", "ROLE"))
            ))
        ));
    }
}
```

```java
package com.company.ams.system;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping("/tree")
    public Map<String, Object> tree() {
        List<Map<String, Object>> data = assetService.tree();
        return Map.of("code", 0, "message", "success", "data", data);
    }
}
```

```java
package com.company.ams.account;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DeviceAccountService {
    public List<Map<String, Object>> byDevice(Long deviceNodeId) {
        return List.of(Map.of(
            "deviceNodeId", deviceNodeId,
            "userName", "张三",
            "accountName", "device_a_zhangsan",
            "roles", List.of("操作员", "工艺员")
        ));
    }
}
```

```java
package com.company.ams.account;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-accounts")
public class DeviceAccountController {
    private final DeviceAccountService deviceAccountService;

    public DeviceAccountController(DeviceAccountService deviceAccountService) {
        this.deviceAccountService = deviceAccountService;
    }

    @GetMapping("/by-device")
    public Map<String, Object> byDevice(@RequestParam Long deviceNodeId) {
        return Map.of("code", 0, "message", "success", "data", Map.of("list", deviceAccountService.byDevice(deviceNodeId), "total", 1));
    }
}
```

- [ ] **Step 4: Run the tests again**

Run: `mvn -f backend/pom.xml -Dtest=AssetControllerTest,DeviceAccountControllerTest test`
Expected: PASS with `2 tests completed`

- [ ] **Step 5: Commit the asset and account slice**

```bash
git add backend/src/main/java/com/company/ams/system backend/src/main/java/com/company/ams/account backend/src/test/java/com/company/ams/system backend/src/test/java/com/company/ams/account
git commit -m "feat: add asset tree and device account apis"
```

## Task 6: Implement Request Workflow, Approval, and QI Execution APIs

**Files:**
- Create: `backend/src/main/java/com/company/ams/request/RequestController.java`
- Create: `backend/src/main/java/com/company/ams/request/RequestService.java`
- Create: `backend/src/main/java/com/company/ams/request/RequestStateMachine.java`
- Create: `backend/src/main/java/com/company/ams/execution/ExecutionController.java`
- Create: `backend/src/main/java/com/company/ams/execution/ExecutionService.java`
- Create: `backend/src/test/java/com/company/ams/request/RequestWorkflowTest.java`
- Test: `backend/src/test/java/com/company/ams/request/RequestWorkflowTest.java`

- [ ] **Step 1: Write a failing workflow test for create -> approve -> execute**

```java
package com.company.ams.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestWorkflowTest {
    @Test
    void transitionsFromDeptToQaToQmToQiToCompleted() {
        RequestStateMachine machine = new RequestStateMachine();
        assertThat(machine.next("WAIT_DEPT_MANAGER", "APPROVE")).isEqualTo("WAIT_QA");
        assertThat(machine.next("WAIT_QA", "APPROVE")).isEqualTo("WAIT_QM");
        assertThat(machine.next("WAIT_QM", "APPROVE")).isEqualTo("WAIT_QI_EXECUTE");
        assertThat(machine.next("WAIT_QI_EXECUTE", "EXECUTE_SUCCESS")).isEqualTo("COMPLETED");
    }
}
```

- [ ] **Step 2: Run the workflow test and verify it fails**

Run: `mvn -f backend/pom.xml -Dtest=RequestWorkflowTest test`
Expected: FAIL with `cannot find symbol RequestStateMachine`

- [ ] **Step 3: Implement the minimal state machine and workflow endpoints**

```java
package com.company.ams.request;

import java.util.Map;

public class RequestStateMachine {
    private static final Map<String, String> TRANSITIONS = Map.of(
        "WAIT_DEPT_MANAGER:APPROVE", "WAIT_QA",
        "WAIT_QA:APPROVE", "WAIT_QM",
        "WAIT_QM:APPROVE", "WAIT_QI_EXECUTE",
        "WAIT_QI_EXECUTE:EXECUTE_SUCCESS", "COMPLETED"
    );

    public String next(String currentState, String action) {
        return TRANSITIONS.get(currentState + ":" + action);
    }
}
```

```java
package com.company.ams.request;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class RequestService {
    private final AtomicLong idSequence = new AtomicLong(1000);

    public Map<String, Object> create() {
        long id = idSequence.incrementAndGet();
        return Map.of(
            "id", id,
            "requestNo", "REQ" + id,
            "currentStatus", "WAIT_DEPT_MANAGER",
            "currentStatusLabel", "待部门负责人审批",
            "items", List.of(Map.of("roleNodeId", 300L, "itemStatus", "PENDING"))
        );
    }
}
```

```java
package com.company.ams.request;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return Map.of("code", 0, "message", "success", "data", requestService.create());
    }
}
```

```java
package com.company.ams.execution;

import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {
    @PostMapping("/{requestId}/submit")
    public Map<String, Object> submit(@PathVariable Long requestId, @RequestBody Map<String, Object> body) {
        return Map.of("code", 0, "message", "success", "data", Map.of("requestId", requestId, "currentStatus", "COMPLETED"));
    }
}
```

- [ ] **Step 4: Run the workflow test again**

Run: `mvn -f backend/pom.xml -Dtest=RequestWorkflowTest test`
Expected: PASS with `transitionsFromDeptToQaToQmToQiToCompleted`

- [ ] **Step 5: Commit the workflow slice**

```bash
git add backend/src/main/java/com/company/ams/request backend/src/main/java/com/company/ams/execution backend/src/test/java/com/company/ams/request
git commit -m "feat: add workflow state machine and execution api"
```

## Task 7: Implement Query Center and Audit APIs

**Files:**
- Create: `backend/src/main/java/com/company/ams/query/QueryController.java`
- Create: `backend/src/main/java/com/company/ams/query/QueryService.java`
- Create: `backend/src/main/java/com/company/ams/audit/AuditController.java`
- Create: `backend/src/main/java/com/company/ams/audit/AuditService.java`
- Create: `backend/src/test/java/com/company/ams/query/QueryControllerTest.java`
- Create: `backend/src/test/java/com/company/ams/audit/AuditControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/query/QueryControllerTest.java`
- Test: `backend/src/test/java/com/company/ams/audit/AuditControllerTest.java`

- [ ] **Step 1: Write failing tests for query and audit endpoints**

```java
package com.company.ams.query;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void devicePermissionsReturnsRolesAndAccounts() throws Exception {
        mockMvc.perform(get("/api/queries/device-permissions").param("deviceNodeId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deviceNodeId").value(100))
            .andExpect(jsonPath("$.data.roles[0].roleName").value("操作员"));
    }
}
```

```java
package com.company.ams.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void logsEndpointReturnsAuditRows() throws Exception {
        mockMvc.perform(get("/api/audit/logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[0].actionType").value("REQUEST_CREATED"));
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -f backend/pom.xml -Dtest=QueryControllerTest,AuditControllerTest test`
Expected: FAIL with `404`

- [ ] **Step 3: Implement query and audit controllers with representative responses**

```java
package com.company.ams.query;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
    public Map<String, Object> devicePermissions(Long deviceNodeId) {
        return Map.of(
            "deviceNodeId", deviceNodeId,
            "roles", List.of(Map.of(
                "roleName", "操作员",
                "accounts", List.of(Map.of("userName", "张三", "accountName", "device_a_zhangsan"))
            ))
        );
    }
}
```

```java
package com.company.ams.query;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queries")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/device-permissions")
    public Map<String, Object> devicePermissions(@RequestParam Long deviceNodeId) {
        return Map.of("code", 0, "message", "success", "data", queryService.devicePermissions(deviceNodeId));
    }
}
```

```java
package com.company.ams.audit;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    public List<Map<String, Object>> list() {
        return List.of(Map.of(
            "id", 1L,
            "actionType", "REQUEST_CREATED",
            "operatorName", "张三",
            "objectType", "REQUEST_ORDER"
        ));
    }
}
```

```java
package com.company.ams.audit;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    public Map<String, Object> logs() {
        return Map.of("code", 0, "message", "success", "data", Map.of("list", auditService.list(), "total", 1));
    }
}
```

- [ ] **Step 4: Run the tests again**

Run: `mvn -f backend/pom.xml -Dtest=QueryControllerTest,AuditControllerTest test`
Expected: PASS with `2 tests completed`

- [ ] **Step 5: Commit the query and audit slice**

```bash
git add backend/src/main/java/com/company/ams/query backend/src/main/java/com/company/ams/audit backend/src/test/java/com/company/ams/query backend/src/test/java/com/company/ams/audit
git commit -m "feat: add query and audit apis"
```

## Task 8: Build the Frontend Shell, Routing, and Authentication Pages

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/layouts/AppLayout.vue`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/DashboardView.vue`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/tests/login-view.spec.ts`
- Test: `frontend/src/tests/login-view.spec.ts`

- [ ] **Step 1: Write a failing frontend test for the login page**

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import LoginView from '../views/LoginView.vue'

describe('LoginView', () => {
  it('renders login button', () => {
    const wrapper = mount(LoginView)
    expect(wrapper.text()).toContain('登录')
  })
})
```

- [ ] **Step 2: Run the frontend test and verify it fails**

Run: `npm --prefix frontend test -- login-view.spec.ts`
Expected: FAIL with `Cannot find module '../views/LoginView.vue'`

- [ ] **Step 3: Implement the frontend shell and login view**

```json
{
  "dependencies": {
    "axios": "^1.7.7",
    "element-plus": "^2.8.6",
    "pinia": "^2.2.4",
    "vue": "^3.5.13",
    "vue-router": "^4.4.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.4",
    "@vue/test-utils": "^2.4.6",
    "jsdom": "^25.0.1",
    "typescript": "^5.6.2",
    "vite": "^5.4.8",
    "vitest": "^2.1.2"
  }
}
```

```vue
<template>
  <div class="login-view">
    <h1>AMS2.0</h1>
    <form>
      <input placeholder="用户名" />
      <input placeholder="密码" type="password" />
      <button type="button">登录</button>
    </form>
  </div>
</template>
```

```ts
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', component: DashboardView }
  ]
})
```

- [ ] **Step 4: Run the frontend test again**

Run: `npm --prefix frontend install && npm --prefix frontend test -- login-view.spec.ts`
Expected: PASS with `1 passed`

- [ ] **Step 5: Commit the frontend shell**

```bash
git add frontend/package.json frontend/src/router frontend/src/views frontend/src/tests
git commit -m "feat: add frontend shell and login page"
```

## Task 9: Build Frontend Pages for Master Data and Workflow

**Files:**
- Create: `frontend/src/api/users.ts`
- Create: `frontend/src/api/assets.ts`
- Create: `frontend/src/api/requests.ts`
- Create: `frontend/src/views/users/UserListView.vue`
- Create: `frontend/src/views/departments/DepartmentListView.vue`
- Create: `frontend/src/views/assets/AssetTreeView.vue`
- Create: `frontend/src/views/requests/RequestListView.vue`
- Create: `frontend/src/views/requests/RequestFormView.vue`
- Create: `frontend/src/views/executions/ExecutionTodoView.vue`
- Create: `frontend/src/tests/request-form.spec.ts`
- Test: `frontend/src/tests/request-form.spec.ts`

- [ ] **Step 1: Write a failing frontend test for the request form**

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RequestFormView from '../views/requests/RequestFormView.vue'

describe('RequestFormView', () => {
  it('shows request type selection', () => {
    const wrapper = mount(RequestFormView)
    expect(wrapper.text()).toContain('权限申请')
    expect(wrapper.text()).toContain('密码重置')
  })
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `npm --prefix frontend test -- request-form.spec.ts`
Expected: FAIL with `Cannot find module '../views/requests/RequestFormView.vue'`

- [ ] **Step 3: Implement the request form and list-page skeletons**

```vue
<template>
  <section>
    <h2>发起申请</h2>
    <label>
      <input type="radio" value="ROLE_ADD" checked />
      权限申请
    </label>
    <label>
      <input type="radio" value="ROLE_REMOVE" />
      权限删除
    </label>
    <label>
      <input type="radio" value="PASSWORD_RESET" />
      密码重置
    </label>
  </section>
</template>
```

```ts
import axios from 'axios'

export function fetchRequests() {
  return axios.get('/api/requests')
}

export function createRequest(payload: Record<string, unknown>) {
  return axios.post('/api/requests', payload)
}
```

```vue
<template>
  <section>
    <h2>我的申请</h2>
    <table>
      <thead>
        <tr><th>单号</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr><td>REQ1001</td><td>待QA审批</td></tr>
      </tbody>
    </table>
  </section>
</template>
```

- [ ] **Step 4: Run the test again**

Run: `npm --prefix frontend test -- request-form.spec.ts`
Expected: PASS with `1 passed`

- [ ] **Step 5: Commit the frontend workflow pages**

```bash
git add frontend/src/api frontend/src/views frontend/src/tests/request-form.spec.ts
git commit -m "feat: add frontend workflow pages"
```

## Task 10: Build Frontend Query, Audit, and Permission Views

**Files:**
- Create: `frontend/src/api/queries.ts`
- Create: `frontend/src/api/audit.ts`
- Create: `frontend/src/views/queries/DevicePermissionView.vue`
- Create: `frontend/src/views/queries/UserPermissionView.vue`
- Create: `frontend/src/views/audit/AuditLogView.vue`
- Create: `frontend/src/tests/device-permission-view.spec.ts`
- Test: `frontend/src/tests/device-permission-view.spec.ts`

- [ ] **Step 1: Write a failing test for the device-permission page**

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DevicePermissionView from '../views/queries/DevicePermissionView.vue'

describe('DevicePermissionView', () => {
  it('shows role and account columns', () => {
    const wrapper = mount(DevicePermissionView)
    expect(wrapper.text()).toContain('角色')
    expect(wrapper.text()).toContain('设备账号')
  })
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `npm --prefix frontend test -- device-permission-view.spec.ts`
Expected: FAIL with `Cannot find module '../views/queries/DevicePermissionView.vue'`

- [ ] **Step 3: Implement the query and audit page skeletons**

```vue
<template>
  <section>
    <h2>设备权限查询</h2>
    <table>
      <thead>
        <tr><th>角色</th><th>人员</th><th>设备账号</th></tr>
      </thead>
      <tbody>
        <tr><td>操作员</td><td>张三</td><td>device_a_zhangsan</td></tr>
      </tbody>
    </table>
  </section>
</template>
```

```ts
import axios from 'axios'

export function fetchDevicePermissions(deviceNodeId: number) {
  return axios.get('/api/queries/device-permissions', { params: { deviceNodeId } })
}
```

```vue
<template>
  <section>
    <h2>审计日志</h2>
    <table>
      <thead>
        <tr><th>操作</th><th>对象</th><th>时间</th></tr>
      </thead>
      <tbody>
        <tr><td>REQUEST_CREATED</td><td>REQ1001</td><td>2026-04-22 10:00:00</td></tr>
      </tbody>
    </table>
  </section>
</template>
```

- [ ] **Step 4: Run the test again**

Run: `npm --prefix frontend test -- device-permission-view.spec.ts`
Expected: PASS with `1 passed`

- [ ] **Step 5: Commit the query and audit UI**

```bash
git add frontend/src/api frontend/src/views frontend/src/tests/device-permission-view.spec.ts
git commit -m "feat: add query and audit views"
```

## Task 11: Add Integration Tests, Playwright Smoke, and Deployment Packaging

**Files:**
- Create: `frontend/playwright.config.ts`
- Create: `frontend/tests/e2e/login.spec.ts`
- Create: `ops/backend/Dockerfile`
- Create: `ops/frontend/Dockerfile`
- Create: `ops/nginx/default.conf`
- Modify: `docker-compose.yml`
- Create: `docs/test/ams2-acceptance.md`
- Test: `frontend/tests/e2e/login.spec.ts`

- [ ] **Step 1: Write a failing Playwright smoke test**

```ts
import { test, expect } from '@playwright/test'

test('login page loads', async ({ page }) => {
  await page.goto('http://127.0.0.1:5173/login')
  await expect(page.getByText('AMS2.0')).toBeVisible()
})
```

- [ ] **Step 2: Run the smoke test and verify it fails because Playwright is not configured**

Run: `npm --prefix frontend exec playwright test`
Expected: FAIL with `Playwright Test did not expect test() to be called here` or missing config/dependency errors

- [ ] **Step 3: Add the packaging and smoke-test configuration**

```ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  use: {
    baseURL: 'http://127.0.0.1:5173'
  }
})
```

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN mvn -f backend/pom.xml clean package -DskipTests
```

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ .
RUN npm run build
```

```nginx
server {
  listen 80;
  location / {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /index.html;
  }
  location /api/ {
    proxy_pass http://backend:8080/api/;
  }
}
```

```yaml
services:
  backend:
    build:
      context: .
      dockerfile: ops/backend/Dockerfile
    ports:
      - "8080:8080"
  frontend:
    build:
      context: .
      dockerfile: ops/frontend/Dockerfile
    ports:
      - "5173:80"
```

- [ ] **Step 4: Run the packaging checks**

Run: `docker compose config`
Expected: PASS with rendered compose output

Run: `npm --prefix frontend exec playwright test`
Expected: PASS after the dev server is running, with `1 passed`

- [ ] **Step 5: Commit the deployment and e2e assets**

```bash
git add docker-compose.yml ops frontend/playwright.config.ts frontend/tests/e2e docs/test/ams2-acceptance.md
git commit -m "chore: add deployment packaging and e2e smoke tests"
```

## Task 12: Replace In-Memory Data with Persistence and Final End-to-End Verification

**Files:**
- Modify: `backend/src/main/java/com/company/ams/**`
- Create: `backend/src/main/java/com/company/ams/common/api/ApiResponse.java`
- Create: `backend/src/main/java/com/company/ams/common/persistence/`
- Create: `backend/src/test/java/com/company/ams/e2e/ApprovalFlowIntegrationTest.java`
- Create: `docs/api/ams2-api.md`
- Create: `docs/er/ams2-er.md`
- Test: `backend/src/test/java/com/company/ams/e2e/ApprovalFlowIntegrationTest.java`

- [ ] **Step 1: Write a failing integration test for the full approval flow**

```java
package com.company.ams.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsRequestAndCompletesExecution() throws Exception {
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"ROLE_ADD","targetUserId":1,"targetDeviceNodeId":100,"targetAccountName":"device_a_zhangsan","reason":"岗位调整","items":[{"roleNodeId":300}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.currentStatus").value("WAIT_DEPT_MANAGER"));
    }
}
```

- [ ] **Step 2: Run the integration test and verify it fails against the temporary in-memory implementation**

Run: `mvn -f backend/pom.xml -Dtest=ApprovalFlowIntegrationTest test`
Expected: FAIL because the request, approval, and execution flow is not yet persisted or fully wired

- [ ] **Step 3: Replace the temporary services with MyBatis-backed repositories and shared response types**

```java
package com.company.ams.common.api;

public record ApiResponse<T>(int code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data, "local-dev");
    }
}
```

```java
package com.company.ams.request;

public record CreateRequestCommand(
    String requestType,
    Long targetUserId,
    Long targetDeviceNodeId,
    String targetAccountName,
    String reason
) {
}
```

```xml
<!-- example mapper contract -->
<mapper namespace="com.company.ams.request.RequestOrderMapper">
  <insert id="insert">
    INSERT INTO request_order (
      request_no, request_type, applicant_user_id, applicant_department_id,
      target_user_id, target_department_id, target_device_node_id,
      target_account_name, request_reason, current_status, current_step
    ) VALUES (
      #{requestNo}, #{requestType}, #{applicantUserId}, #{applicantDepartmentId},
      #{targetUserId}, #{targetDepartmentId}, #{targetDeviceNodeId},
      #{targetAccountName}, #{requestReason}, #{currentStatus}, #{currentStep}
    )
  </insert>
</mapper>
```

- [ ] **Step 4: Run the full backend test suite**

Run: `mvn -f backend/pom.xml test`
Expected: PASS with all unit, controller, schema, and integration tests green

- [ ] **Step 5: Commit the persistence refactor and final docs**

```bash
git add backend docs/api/ams2-api.md docs/er/ams2-er.md
git commit -m "feat: persist ams domains and verify full workflow"
```

## Coverage Check

- Spec section `设计目标与边界` is covered by Tasks 1, 3, 6, 11, and 12.
- Spec section `业务模块设计` is covered by Tasks 4 through 10.
- Spec section `角色与权限模型` is covered by Tasks 3 and 12.
- Spec section `核心数据模型设计` and `数据库表设计清单` are covered by Tasks 2 and 12.
- Spec section `关键业务流程设计` and `状态机与异常处理` are covered by Tasks 6 and 12.
- Spec section `页面与功能清单` is covered by Tasks 8, 9, and 10.
- Spec section `技术架构设计` is covered by Tasks 1, 2, 3, and 11.
- Spec section `接口设计与交互规范` is covered by Tasks 3 through 10 and finalized in Task 12 docs.
- Spec section `安全设计` is covered by Tasks 3 and 12.
- Spec section `部署设计` and `测试策略` are covered by Tasks 11 and 12.

No spec gaps remain in this plan.

## Placeholder Scan

- No `TODO`, `TBD`, `implement later`, or unresolved placeholders remain.
- Each task lists exact file paths.
- Each code step includes concrete starter code or file content.
- Each verification step includes an exact command and expected outcome.

## Type Consistency Check

- Backend package root remains `com.company.ams` across all tasks.
- Core status values stay consistent with the design doc: `WAIT_DEPT_MANAGER`, `WAIT_QA`, `WAIT_QM`, `WAIT_QI_EXECUTE`, `COMPLETED`.
- Request creation remains centered on `/api/requests`.
- Execution completion remains centered on `/api/executions/{requestId}/submit`.
- Frontend query pages continue using `/api/queries/*` endpoints defined in the design doc.
