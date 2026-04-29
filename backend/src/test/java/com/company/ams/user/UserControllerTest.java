package com.company.ams.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void listUsersReturnsRows() throws Exception {
        given(userService.list()).willReturn(List.of(
                new UserRow(1L, "EMP001", "Zhang San", 1L, "Assembly Dept", "ACTIVE", "zhangsan", "ENABLED"),
                new UserRow(2L, "EMP002", "Li Si", 2L, "Quality Dept", "ACTIVE", "lisi", "DISABLED")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list[0].userCode").value("EMP001"))
                .andExpect(jsonPath("$.data.list[0].userName").value("Zhang San"))
                .andExpect(jsonPath("$.data.list[1].departmentName").value("Quality Dept"))
                .andExpect(jsonPath("$.data.list[1].accountStatus").value("DISABLED"));
    }

    @Test
    @WithMockUser
    void createUserReturnsPayloadIncludingUserCodeAndDepartmentId() throws Exception {
        given(userService.create(any(UserUpsertCommand.class))).willAnswer(invocation -> {
            UserUpsertCommand command = invocation.getArgument(0, UserUpsertCommand.class);
            Assertions.assertEquals("ENABLED", command.accountStatus());
            return new UserRow(5L, "EMP005", "New User", 1L, "Assembly Dept", "ACTIVE", "newuser", "ENABLED");
        });

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userCode": "EMP005",
                                  "userName": "New User",
                                  "departmentId": 1,
                                  "employmentStatus": "ACTIVE",
                                  "loginName": "newuser"
                                  ,"accountStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userCode").value("EMP005"))
                .andExpect(jsonPath("$.data.departmentId").value(1));
    }

    @Test
    @WithMockUser
    void patchStatusReturnsUpdatedAccountStatus() throws Exception {
        given(userService.updateStatus(eq(2L), any(UserStatusCommand.class))).willReturn(
                new UserRow(2L, "EMP001", "Zhang San", 1L, "Assembly Dept", "ACTIVE", "zhangsan", "DISABLED"));

        mockMvc.perform(patch("/api/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountStatus":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accountStatus").value("DISABLED"));
    }
}
