package com.company.ams.audit;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import com.company.ams.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    @WithMockUser
    void listLogsReturnsWrappedListPayload() throws Exception {
        given(auditService.logs()).willReturn(new AuditLogListPayload(
                List.of(new AuditLogRow(
                        1L,
                        "REQUEST_CREATED",
                        "王五",
                        "REQUEST",
                        "2026-04-22T09:30:00Z"
                )),
                1
        ));

        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].actionType").value("REQUEST_CREATED"))
                .andExpect(jsonPath("$.data.list[0].operatorName").value("王五"))
                .andExpect(jsonPath("$.data.list[0].objectType").value("REQUEST"))
                .andExpect(jsonPath("$.data.list[0].createdAt").value("2026-04-22T09:30:00Z"));
    }
}
