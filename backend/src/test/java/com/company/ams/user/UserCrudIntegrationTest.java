package com.company.ams.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-crud;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteRejectsWhenUserStillOwnsDeviceAccounts() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(delete("/api/users/2").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("User still owns device accounts"));
    }

    @Test
    void deleteRejectsWhenUserStillHasUnfinishedRequests() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP998",
                                  "userName": "Request Blocked",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "requestblocked",
                                  "accountStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk());

        Long userId = jdbcTemplate.queryForObject(
                "select id from sys_user where login_name = ?",
                Long.class,
                "requestblocked");

        jdbcTemplate.update(
                """
                insert into request_order (
                  request_no,
                  request_type,
                  applicant_user_id,
                  applicant_department_id,
                  target_user_id,
                  target_department_id,
                  target_device_node_id,
                  target_account_name,
                  request_reason,
                  current_status,
                  current_step,
                  created_at,
                  updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                "REQ-UNFINISHED-REJECTED",
                "ROLE_ADD",
                userId,
                1L,
                2L,
                1L,
                100L,
                "device_a_zhangsan",
                "seed-rejected",
                "REJECTED",
                "REJECTED");

        mockMvc.perform(delete("/api/users/" + userId).session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("User still has unfinished requests"));
    }

    @Test
    void createPersistsAccountStatusFromCommand() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP999",
                                  "userName": "Spec User",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "specuser",
                                  "accountStatus": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userCode").value("EMP999"))
                .andExpect(jsonPath("$.data.accountStatus").value("DISABLED"));
    }

    @Test
    void patchStatusRejectsInvalidAccountStatus() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(patch("/api/users/2/status")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {"accountStatus":"LOCKED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("accountStatus must be ENABLED or DISABLED"));
    }

    @Test
    void updatePersistsAccountStatusFromCommand() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(put("/api/users/4")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP003",
                                  "userName": "Wang Wu Updated",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "wangwu",
                                  "accountStatus": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.accountStatus").value("DISABLED"));
    }

    @Test
    void createRejectsDuplicateUserCode() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP001",
                                  "userName": "Dup Code",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "dupcode",
                                  "accountStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("User code already exists"));
    }

    @Test
    void createRejectsDuplicateUserCodeEvenIfExistingUserIsSoftDeleted() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP997",
                                  "userName": "Soft Delete First",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "softdel1",
                                  "accountStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long userId = jdbcTemplate.queryForObject(
                "select id from sys_user where login_name = ?",
                Long.class,
                "softdel1");

        mockMvc.perform(delete("/api/users/" + userId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Service precheck ignores deleted=1 records, but the DB unique constraint should still block duplicates.
        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP997",
                                  "userName": "Soft Delete Second",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "softdel2",
                                  "accountStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("User code already exists"));
    }

    @Test
    void createRejectsInvalidDepartmentId() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userCode": "EMP996",
                                  "userName": "Bad Dept",
                                  "departmentId": 9999,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "baddept",
                                  "accountStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("departmentId does not exist"));
    }

    @Test
    void createWithMissingBodyReturnsApiResponseEnvelope() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("Request body is required"));
    }

    private MockHttpSession login(String loginName, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"loginName":"%s","password":"%s"}
                            """.formatted(loginName, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
