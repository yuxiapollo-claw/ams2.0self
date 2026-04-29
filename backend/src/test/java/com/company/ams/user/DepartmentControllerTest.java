package com.company.ams.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.company.ams.auth.SecurityConfig;

@WebMvcTest(DepartmentController.class)
@Import(SecurityConfig.class)
class DepartmentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @Test
    @WithMockUser
    void createDepartmentReturnsExpandedRowShape() throws Exception {
        given(departmentService.create(any(DepartmentUpsertCommand.class))).willAnswer(invocation -> {
            DepartmentUpsertCommand command = invocation.getArgument(0, DepartmentUpsertCommand.class);
            Assertions.assertEquals("Packaging Dept", command.departmentName());
            Assertions.assertEquals(2L, command.managerUserId());
            Assertions.assertEquals("Handles packaging", command.description());
            Assertions.assertEquals("ENABLED", command.status());
            return new DepartmentRow(
                    5L,
                    "Packaging Dept",
                    2L,
                    "Zhang San",
                    "Handles packaging",
                    0,
                    "ENABLED",
                    "2026-04-24T16:00:00");
        });

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "Packaging Dept",
                                  "managerUserId": 2,
                                  "description": "Handles packaging",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.departmentName").value("Packaging Dept"))
                .andExpect(jsonPath("$.data.managerUserId").value(2))
                .andExpect(jsonPath("$.data.managerUserName").value("Zhang San"))
                .andExpect(jsonPath("$.data.description").value("Handles packaging"))
                .andExpect(jsonPath("$.data.memberCount").value(0))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-04-24T16:00:00"));
    }
}
