package com.company.ams.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.execution.ExecutionService;
import com.company.ams.request.RequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "spring.datasource.url=jdbc:h2:mem:approval-flow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApprovalFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void requestLifecyclePersistsAcrossCreationApprovalAndExecution() throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");

        MvcResult mvcResult = mockMvc.perform(post("/api/requests")
                .session(session)
                .contentType("application/json")
                .content("""
                    {
                      "requestType":"ROLE_ADD",
                      "targetUserId":4,
                      "targetDeviceNodeId":100,
                      "targetAccountName":"device_a_wangwu",
                      "reason":"岗位调整",
                      "items":[{"roleNodeId":302}]
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStatus").value("WAIT_DEPT_MANAGER"))
                .andExpect(jsonPath("$.data.items[0].roleNodeId").value(302))
                .andReturn();

        JsonNode body = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        long requestId = body.path("data").path("id").asLong();

        assertThat(currentStatus(requestId)).isEqualTo("WAIT_DEPT_MANAGER");
        assertThat(requestSnapshot(requestId).applicantUserId()).isEqualTo(2L);
        assertThat(requestSnapshot(requestId).requestType()).isEqualTo("ROLE_ADD");
        assertThat(requestSnapshot(requestId).requestReason()).isEqualTo("岗位调整");

        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        executionService.submit(requestId);

        assertThat(currentStatus(requestId)).isEqualTo("COMPLETED");
        assertThat(sourceRoleBindings(requestId)).isEqualTo(1);
        assertThat(grantedRoleBindings(requestId, 4L, 302L)).isEqualTo(1);
    }

    @Test
    void requestLifecycleBindsUnboundTargetAccountDuringExecution() throws Exception {
        jdbcTemplate.update(
                """
                insert into device_account (
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  created_at,
                  updated_at,
                  deleted
                ) values (null, ?, ?, 'ENABLED', 'MANUAL', 'Spare account', current_timestamp, current_timestamp, 0)
                """,
                100L,
                "device_a_spare_01");

        MockHttpSession session = login("zhangsan", "zhangsan123");

        MvcResult mvcResult = mockMvc.perform(post("/api/requests")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestType":"ROLE_ADD",
                                  "targetUserId":4,
                                  "targetDeviceNodeId":100,
                                  "targetAccountName":"device_a_spare_01",
                                  "reason":"bind-unbound-account",
                                  "items":[{"roleNodeId":302}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStatus").value("WAIT_DEPT_MANAGER"))
                .andReturn();

        long requestId = objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        executionService.submit(requestId);

        assertThat(currentStatus(requestId)).isEqualTo("COMPLETED");
        assertThat(boundUserId("device_a_spare_01")).isEqualTo(4L);
        assertThat(grantedRoleBindingsForAccount(requestId, "device_a_spare_01", 302L)).isEqualTo(1);
    }

    @Test
    void repeatedGrantPreservesOriginalSourceRequestId() throws Exception {
        String accountName = uniqueName("device_a_repeat_");
        jdbcTemplate.update(
                """
                insert into device_account (
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  created_at,
                  updated_at,
                  deleted
                ) values (?, ?, ?, 'ENABLED', 'MANUAL', 'Repeat grant account', current_timestamp, current_timestamp, 0)
                """,
                4L,
                100L,
                accountName);

        long firstRequestId = createAndExecuteRoleRequest(accountName, 4L, 302L);
        long secondRequestId = createAndExecuteRoleRequest(accountName, 4L, 302L);

        assertThat(roleBindingSourceRequestId(accountName, 302L)).isEqualTo(firstRequestId);
        assertThat(sourceRoleBindings(secondRequestId)).isEqualTo(0);
        assertThat(deviceAccountRoleCount(accountName, 302L)).isEqualTo(1);
    }

    @Test
    void requestCreationRejectsAccountBoundToDifferentUser() throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");

        mockMvc.perform(post("/api/requests")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestType":"ROLE_ADD",
                                  "targetUserId":4,
                                  "targetDeviceNodeId":100,
                                  "targetAccountName":"device_a_lisi",
                                  "reason":"should-fail",
                                  "items":[{"roleNodeId":302}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Target device account is already bound to a different user"));
    }

    @Test
    void deviceAccountCanBeRecreatedWithSameNameAfterSoftDelete() throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");
        String accountName = uniqueName("device_a_reuse_");

        long firstAccountId = createDeviceAccount(session, accountName);
        for (int attempt = 0; attempt <= 1000; attempt++) {
            insertLiveDeviceAccount(buildDeletedAccountName(accountName, firstAccountId, attempt));
        }

        mockMvc.perform(delete("/api/device-accounts/{deviceAccountId}", firstAccountId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        long secondAccountId = createDeviceAccount(session, accountName);

        assertThat(secondAccountId).isNotEqualTo(firstAccountId);
        assertThat(liveDeviceAccountCount(accountName)).isEqualTo(1);
        assertThat(deletedTombstoneCount(accountName)).isEqualTo(1);
        assertThat(deviceAccountName(firstAccountId)).isEqualTo(buildDeletedAccountName(accountName, firstAccountId, 1001));
    }

    @Test
    void deviceAccountCreateRejectsOverlongAccountName() throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");
        String accountName = "a".repeat(101);

        mockMvc.perform(post("/api/device-accounts")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "deviceNodeId": 100,
                                  "userId": null,
                                  "accountName": "%s",
                                  "accountStatus": "ENABLED",
                                  "sourceType": "MANUAL",
                                  "remark": "Too long"
                                }
                                """.formatted(accountName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("accountName must be at most 100 characters"));
    }

    @Test
    void requestListReturnsPersistedRequestSummary() throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");

        mockMvc.perform(post("/api/requests")
                .session(session)
                .contentType("application/json")
                .content("""
                    {
                      "requestType":"ROLE_ADD",
                      "targetUserId":4,
                      "targetDeviceNodeId":100,
                      "targetAccountName":"device_a_wangwu",
                      "reason":"request-list-check",
                      "items":[{"roleNodeId":301}]
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/requests").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[0].requestType").value("ROLE_ADD"))
            .andExpect(jsonPath("$.data.list[0].targetAccountName").value("device_a_wangwu"))
            .andExpect(jsonPath("$.data.list[0].currentStatus").value("WAIT_DEPT_MANAGER"))
            .andExpect(jsonPath("$.data.list[0].reason").value("request-list-check"));
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

    private long createAndExecuteRoleRequest(String accountName, long targetUserId, long roleNodeId) throws Exception {
        MockHttpSession session = login("zhangsan", "zhangsan123");
        MvcResult mvcResult = mockMvc.perform(post("/api/requests")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "requestType":"ROLE_ADD",
                                  "targetUserId":%d,
                                  "targetDeviceNodeId":100,
                                  "targetAccountName":"%s",
                                  "reason":"repeat-grant",
                                  "items":[{"roleNodeId":%d}]
                                }
                                """.formatted(targetUserId, accountName, roleNodeId)))
                .andExpect(status().isOk())
                .andReturn();

        long requestId = objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        requestService.advance(requestId, "APPROVE");
        executionService.submit(requestId);
        return requestId;
    }

    private long createDeviceAccount(MockHttpSession session, String accountName) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/device-accounts")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "deviceNodeId": 100,
                                  "userId": null,
                                  "accountName": "%s",
                                  "accountStatus": "ENABLED",
                                  "sourceType": "MANUAL",
                                  "remark": "Reusable account"
                                }
                                """.formatted(accountName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
    }

    private void insertLiveDeviceAccount(String accountName) {
        jdbcTemplate.update(
                """
                insert into device_account (
                  user_id,
                  device_node_id,
                  account_name,
                  account_status,
                  source_type,
                  remark,
                  created_at,
                  updated_at,
                  deleted
                ) values (null, 100, ?, 'ENABLED', 'MANUAL', 'Conflict row', current_timestamp, current_timestamp, 0)
                """,
                accountName);
    }

    private String currentStatus(long requestId) {
        return jdbcTemplate.queryForObject(
                "select current_status from request_order where id = ?",
                String.class,
                requestId);
    }

    private RequestSnapshot requestSnapshot(long requestId) {
        return jdbcTemplate.queryForObject(
                """
                select applicant_user_id, request_type, request_reason
                from request_order
                where id = ?
                """,
                (rs, rowNum) -> new RequestSnapshot(
                        rs.getLong("applicant_user_id"),
                        rs.getString("request_type"),
                        rs.getString("request_reason")),
                requestId);
    }

    private int sourceRoleBindings(long requestId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from device_account_role where source_request_id = ?",
                Integer.class,
                requestId);
        return count == null ? 0 : count;
    }

    private Long roleBindingSourceRequestId(String accountName, long roleNodeId) {
        return jdbcTemplate.queryForObject(
                """
                select dar.source_request_id
                from device_account_role dar
                join device_account da on da.id = dar.device_account_id
                where da.account_name = ?
                  and dar.role_node_id = ?
                """,
                Long.class,
                accountName,
                roleNodeId);
    }

    private int deviceAccountRoleCount(String accountName, long roleNodeId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account_role dar
                join device_account da on da.id = dar.device_account_id
                where da.account_name = ?
                  and dar.role_node_id = ?
                """,
                Integer.class,
                accountName,
                roleNodeId);
        return count == null ? 0 : count;
    }

    private int grantedRoleBindings(long requestId, long targetUserId, long roleNodeId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account_role dar
                join device_account da on da.id = dar.device_account_id
                where dar.source_request_id = ?
                  and da.user_id = ?
                  and dar.role_node_id = ?
                """,
                Integer.class,
                requestId,
                targetUserId,
                roleNodeId);
        return count == null ? 0 : count;
    }

    private Long boundUserId(String accountName) {
        return jdbcTemplate.queryForObject(
                """
                select user_id
                from device_account
                where account_name = ?
                """,
                Long.class,
                accountName);
    }

    private int grantedRoleBindingsForAccount(long requestId, String accountName, long roleNodeId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account_role dar
                join device_account da on da.id = dar.device_account_id
                where dar.source_request_id = ?
                  and da.account_name = ?
                  and dar.role_node_id = ?
                """,
                Integer.class,
                requestId,
                accountName,
                roleNodeId);
        return count == null ? 0 : count;
    }

    private int liveDeviceAccountCount(String accountName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account
                where account_name = ?
                  and deleted = 0
                """,
                Integer.class,
                accountName);
        return count == null ? 0 : count;
    }

    private int deletedTombstoneCount(String accountName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account
                where deleted = 1
                  and account_name like ?
                """,
                Integer.class,
                accountName + "%__deleted__%");
        return count == null ? 0 : count;
    }

    private String deviceAccountName(long deviceAccountId) {
        return jdbcTemplate.queryForObject(
                """
                select account_name
                from device_account
                where id = ?
                """,
                String.class,
                deviceAccountId);
    }

    private String uniqueName(String prefix) {
        return prefix + System.nanoTime();
    }

    private String buildDeletedAccountName(String accountName, long deviceAccountId, int attempt) {
        String suffix = "__deleted__" + deviceAccountId + "__" + Integer.toString(attempt, 36);
        int prefixLength = Math.max(0, 100 - suffix.length());
        return accountName.substring(0, Math.min(accountName.length(), prefixLength)) + suffix;
    }

    private record RequestSnapshot(long applicantUserId, String requestType, String requestReason) {}
}
