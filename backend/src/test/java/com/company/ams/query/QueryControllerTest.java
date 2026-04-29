package com.company.ams.query;

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

@WebMvcTest(QueryController.class)
@Import(SecurityConfig.class)
class QueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryService queryService;

    @Test
    @WithMockUser
    void devicePermissionsReturnsWrappedNestedPayload() throws Exception {
        given(queryService.devicePermissions(100L)).willReturn(new DevicePermissionPayload(
                100L,
                List.of(
                        new DevicePermissionRole(
                                "操作员",
                                List.of(
                                        new DevicePermissionAccount("张三", "device_a_zhangsan"),
                                        new DevicePermissionAccount("李四", "device_a_lisi")
                                )
                        )
                )
        ));

        mockMvc.perform(get("/api/queries/device-permissions").param("deviceNodeId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.deviceNodeId").value(100))
                .andExpect(jsonPath("$.data.roles[0].roleName").value("操作员"))
                .andExpect(jsonPath("$.data.roles[0].accounts[0].userName").value("张三"))
                .andExpect(jsonPath("$.data.roles[0].accounts[0].accountName").value("device_a_zhangsan"))
                .andExpect(jsonPath("$.data.roles[0].accounts[1].userName").value("李四"))
                .andExpect(jsonPath("$.data.roles[0].accounts[1].accountName").value("device_a_lisi"));
    }

    @Test
    @WithMockUser
    void devicePermissionsRequiresDeviceNodeId() throws Exception {
        mockMvc.perform(get("/api/queries/device-permissions"))
                .andExpect(status().isBadRequest());
    }
}
