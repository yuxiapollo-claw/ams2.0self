package com.company.ams.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "spring.datasource.url=jdbc:h2:mem:dashboard-controller;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DashboardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void summaryReturnsSeededMetricsAndCards() throws Exception {
        seedRequests();
        MockHttpSession session = login("zhangsan", "zhangsan123");

        mockMvc.perform(get("/api/dashboard/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.metrics.userTotal").value(4))
                .andExpect(jsonPath("$.data.metrics.departmentTotal").value(2))
                .andExpect(jsonPath("$.data.metrics.deviceAccountTotal").value(3))
                .andExpect(jsonPath("$.data.metrics.pendingRequestTotal").value(2))
                .andExpect(jsonPath("$.data.alerts").isArray())
                .andExpect(jsonPath("$.data.alerts[0].alertKey").value("unbound-accounts"))
                .andExpect(jsonPath("$.data.alerts[0].count").value(0))
                .andExpect(jsonPath("$.data.alerts[1].alertKey").value("no-active-roles"))
                .andExpect(jsonPath("$.data.alerts[1].count").value(1))
                .andExpect(jsonPath("$.data.recentRequests").isArray())
                .andExpect(jsonPath("$.data.recentRequests.length()").value(2))
                .andExpect(jsonPath("$.data.recentRequests[0].requestNo").value("REQ-NEW"))
                .andExpect(jsonPath("$.data.recentRequests[0].currentStatus").value("WAIT_QA"))
                .andExpect(jsonPath("$.data.recentRequests[1].requestNo").value("REQ-OLD"))
                .andExpect(jsonPath("$.data.recentRequests[1].currentStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.quickActions").isArray())
                .andExpect(jsonPath("$.data.quickActions[0].actionKey").value("create-request"));
    }

    private void seedRequests() {
        jdbcTemplate.update(
                """
                insert into request_order (
                  id,
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
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                10L,
                "REQ-OLD",
                "ROLE_ADD",
                2L,
                1L,
                4L,
                1L,
                100L,
                "device_a_wangwu",
                "seed-old",
                "REJECTED",
                "WAIT_QA",
                java.sql.Timestamp.valueOf("2026-04-20 10:00:00"),
                java.sql.Timestamp.valueOf("2026-04-20 10:00:00"));

        jdbcTemplate.update(
                """
                insert into request_order (
                  id,
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
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                11L,
                "REQ-NEW",
                "ROLE_ADD",
                2L,
                1L,
                4L,
                1L,
                100L,
                "device_a_wangwu",
                "seed-new",
                "WAIT_QA",
                "WAIT_QA",
                java.sql.Timestamp.valueOf("2026-04-23 10:00:00"),
                java.sql.Timestamp.valueOf("2026-04-23 10:00:00"));
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
