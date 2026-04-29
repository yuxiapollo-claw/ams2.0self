package com.company.ams.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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
        "spring.datasource.url=jdbc:h2:mem:department-crud;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DepartmentCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteRejectsDepartmentWithActiveMembers() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(delete("/api/departments/1").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("Department still has members"));
    }

    @Test
    void deleteRejectsDepartmentWithInactiveButNonDeletedMembersAndListCountsThem() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        jdbcTemplate.update(
                """
                insert into sys_department (id, department_name, manager_user_id, description, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                """,
                9L,
                "Dormant Dept",
                null,
                "Has inactive member",
                "ENABLED");

        jdbcTemplate.update(
                """
                insert into sys_user (
                  id,
                  user_code,
                  user_name,
                  department_id,
                  employment_status,
                  login_name,
                  password_hash,
                  account_status,
                  created_at,
                  updated_at,
                  deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                """,
                19L,
                "EMP019",
                "Dormant User",
                9L,
                "INACTIVE",
                "dormantuser",
                "$2a$10$mMQ4LA/fmGkbcXoS0ZITM.QOjGsgIo9ECYRHi2ucTBOdmI/BvCyPq",
                "DISABLED");

        mockMvc.perform(get("/api/departments").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[2].departmentName").value("Dormant Dept"))
                .andExpect(jsonPath("$.data.list[2].memberCount").value(1));

        mockMvc.perform(delete("/api/departments/9").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("Department still has members"));
    }

    @Test
    void createAndUpdatePersistDescriptionManagerAndStatus() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/departments")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "departmentName": "Packaging Dept",
                                  "managerUserId": 2,
                                  "description": "Handles packaging",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.departmentName").value("Packaging Dept"))
                .andExpect(jsonPath("$.data.managerUserId").value(2))
                .andExpect(jsonPath("$.data.managerUserName").value("Zhang San"))
                .andExpect(jsonPath("$.data.description").value("Handles packaging"))
                .andExpect(jsonPath("$.data.memberCount").value(0))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        Long departmentId = jdbcTemplate.queryForObject(
                "select id from sys_department where department_name = ?",
                Long.class,
                "Packaging Dept");

        mockMvc.perform(put("/api/departments/" + departmentId)
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "departmentName": "Packaging Operations",
                                  "managerUserId": 3,
                                  "description": "Updated description",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(departmentId))
                .andExpect(jsonPath("$.data.departmentName").value("Packaging Operations"))
                .andExpect(jsonPath("$.data.managerUserId").value(3))
                .andExpect(jsonPath("$.data.managerUserName").value("Li Si"))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.memberCount").value(0))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                """
                select department_name, manager_user_id, description, status, deleted
                from sys_department
                where id = ?
                """,
                departmentId);

        org.junit.jupiter.api.Assertions.assertEquals("Packaging Operations", persisted.get("department_name"));
        org.junit.jupiter.api.Assertions.assertEquals(3L, ((Number) persisted.get("manager_user_id")).longValue());
        org.junit.jupiter.api.Assertions.assertEquals("Updated description", persisted.get("description"));
        org.junit.jupiter.api.Assertions.assertEquals("ENABLED", persisted.get("status"));
        org.junit.jupiter.api.Assertions.assertEquals(0, ((Number) persisted.get("deleted")).intValue());
    }

    @Test
    void updatePreservesNonDeletedManagerThatIsNotActiveOrEnabled() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        jdbcTemplate.update(
                """
                insert into sys_department (id, department_name, manager_user_id, description, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                """,
                10L,
                "Legacy Managed Dept",
                20L,
                "Original description",
                "ENABLED");

        jdbcTemplate.update(
                """
                insert into sys_user (
                  id,
                  user_code,
                  user_name,
                  department_id,
                  employment_status,
                  login_name,
                  password_hash,
                  account_status,
                  created_at,
                  updated_at,
                  deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                """,
                20L,
                "EMP020",
                "Legacy Manager",
                10L,
                "INACTIVE",
                "legacymanager",
                "$2a$10$mMQ4LA/fmGkbcXoS0ZITM.QOjGsgIo9ECYRHi2ucTBOdmI/BvCyPq",
                "DISABLED");

        mockMvc.perform(put("/api/departments/10")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "departmentName": "Legacy Managed Dept Updated",
                                  "managerUserId": 20,
                                  "description": "Description only change",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.managerUserId").value(20))
                .andExpect(jsonPath("$.data.managerUserName").value("Legacy Manager"))
                .andExpect(jsonPath("$.data.description").value("Description only change"));
    }

    @Test
    void createRejectsDescriptionLongerThan255Characters() throws Exception {
        MockHttpSession session = login("admin", "admin123");
        String description = "x".repeat(256);

        mockMvc.perform(post("/api/departments")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "departmentName": "Too Long Dept",
                                  "description": "%s",
                                  "status": "ENABLED"
                                }
                                """.formatted(description)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("description must be at most 255 characters"));
    }

    @Test
    void createRejectsDepartmentNameLongerThan100Characters() throws Exception {
        MockHttpSession session = login("admin", "admin123");
        String departmentName = "n".repeat(101);

        mockMvc.perform(post("/api/departments")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "departmentName": "%s",
                                  "status": "ENABLED"
                                }
                                """.formatted(departmentName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("departmentName must be at most 100 characters"));
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
