package com.company.ams.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.auth.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeviceAccountController.class)
@Import(SecurityConfig.class)
class DeviceAccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceAccountService deviceAccountService;

    @Test
    @WithMockUser
    void listReturnsRichRowsIncludingUnboundAccounts() throws Exception {
        given(deviceAccountService.list()).willReturn(List.of(
                new DeviceAccountRow(
                        1L,
                        100L,
                        "Device A",
                        2L,
                        "Zhang San",
                        "device_a_zhangsan",
                        "ENABLED",
                        "MANUAL",
                        "Primary",
                        List.of("Operator", "Technician")),
                new DeviceAccountRow(
                        4L,
                        100L,
                        "Device A",
                        null,
                        null,
                        "device_a_spare_01",
                        "DISABLED",
                        "IMPORTED",
                        "Spare",
                        List.of())));

        mockMvc.perform(get("/api/device-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].deviceNodeId").value(100))
                .andExpect(jsonPath("$.data.list[0].deviceName").value("Device A"))
                .andExpect(jsonPath("$.data.list[0].userId").value(2))
                .andExpect(jsonPath("$.data.list[0].userName").value("Zhang San"))
                .andExpect(jsonPath("$.data.list[0].accountName").value("device_a_zhangsan"))
                .andExpect(jsonPath("$.data.list[0].accountStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.list[0].sourceType").value("MANUAL"))
                .andExpect(jsonPath("$.data.list[0].remark").value("Primary"))
                .andExpect(jsonPath("$.data.list[0].roles[0]").value("Operator"))
                .andExpect(jsonPath("$.data.list[0].roles[1]").value("Technician"))
                .andExpect(jsonPath("$.data.list[1].id").value(4))
                .andExpect(jsonPath("$.data.list[1].userId").isEmpty())
                .andExpect(jsonPath("$.data.list[1].userName").isEmpty())
                .andExpect(jsonPath("$.data.list[1].accountName").value("device_a_spare_01"))
                .andExpect(jsonPath("$.data.list[1].roles").isArray());
    }

    @Test
    @WithMockUser
    void byDeviceReturnsRichRowsAndDerivedTotal() throws Exception {
        given(deviceAccountService.byDevice(100L)).willReturn(List.of(
                new DeviceAccountRow(
                        1L,
                        100L,
                        "Device A",
                        2L,
                        "Zhang San",
                        "device_a_zhangsan",
                        "ENABLED",
                        "MANUAL",
                        "Primary",
                        List.of("Operator", "Technician")),
                new DeviceAccountRow(
                        4L,
                        100L,
                        "Device A",
                        null,
                        null,
                        "device_a_spare_01",
                        "DISABLED",
                        "IMPORTED",
                        "Spare",
                        List.of())));

        mockMvc.perform(get("/api/device-accounts/by-device").param("deviceNodeId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].deviceName").value("Device A"))
                .andExpect(jsonPath("$.data.list[1].userId").isEmpty())
                .andExpect(jsonPath("$.data.list[1].accountName").value("device_a_spare_01"));
    }

    @Test
    @WithMockUser
    void createAllowsNullUserIdAndReturnsNullableBindingRow() throws Exception {
        given(deviceAccountService.create(any(DeviceAccountUpsertCommand.class))).willReturn(
                new DeviceAccountRow(
                        9L,
                        100L,
                        "Device A",
                        null,
                        null,
                        "device_a_spare_09",
                        "ENABLED",
                        "MANUAL",
                        "Spare",
                        List.of()));

        mockMvc.perform(post("/api/device-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceNodeId": 100,
                                  "userId": null,
                                  "accountName": "device_a_spare_09",
                                  "accountStatus": "ENABLED",
                                  "sourceType": "MANUAL",
                                  "remark": "Spare"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.deviceNodeId").value(100))
                .andExpect(jsonPath("$.data.deviceName").value("Device A"))
                .andExpect(jsonPath("$.data.userId").isEmpty())
                .andExpect(jsonPath("$.data.userName").isEmpty())
                .andExpect(jsonPath("$.data.accountName").value("device_a_spare_09"))
                .andExpect(jsonPath("$.data.accountStatus").value("ENABLED"))
                .andExpect(jsonPath("$.data.sourceType").value("MANUAL"))
                .andExpect(jsonPath("$.data.remark").value("Spare"))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @WithMockUser
    void updateReturnsRichRow() throws Exception {
        given(deviceAccountService.update(eq(9L), any(DeviceAccountUpsertCommand.class))).willReturn(
                new DeviceAccountRow(
                        9L,
                        100L,
                        "Device A",
                        4L,
                        "Wang Wu",
                        "device_a_spare_09",
                        "ENABLED",
                        "MANUAL",
                        "Assigned",
                        List.of("Inspector")));

        mockMvc.perform(put("/api/device-accounts/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceNodeId": 100,
                                  "userId": 4,
                                  "accountName": "device_a_spare_09",
                                  "accountStatus": "ENABLED",
                                  "sourceType": "MANUAL",
                                  "remark": "Assigned"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(4))
                .andExpect(jsonPath("$.data.userName").value("Wang Wu"))
                .andExpect(jsonPath("$.data.roles[0]").value("Inspector"));
    }

    @Test
    @WithMockUser
    void deleteReturnsSuccessEnvelope() throws Exception {
        mockMvc.perform(delete("/api/device-accounts/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser
    void byDeviceRequiresDeviceNodeId() throws Exception {
        mockMvc.perform(get("/api/device-accounts/by-device"))
                .andExpect(status().isBadRequest());
    }
}
