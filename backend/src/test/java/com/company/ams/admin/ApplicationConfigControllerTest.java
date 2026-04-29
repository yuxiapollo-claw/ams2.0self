package com.company.ams.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.auth.SecurityConfig;
import com.company.ams.common.api.ListPayload;
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

@WebMvcTest(ApplicationConfigController.class)
@Import(SecurityConfig.class)
class ApplicationConfigControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationConfigService applicationConfigService;

    @Test
    @WithMockUser
    void listReturnsApplicationConfigRows() throws Exception {
        given(applicationConfigService.list()).willReturn(List.of(
                new ApplicationConfigRow(1L, "DMS", "APP-DMS", "Document system", "ENABLED", "2026-04-29T10:00:00")));

        mockMvc.perform(get("/api/admin/application-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1))
                .andExpect(jsonPath("$.data.list[0].applicationName").value("DMS"))
                .andExpect(jsonPath("$.data.list[0].applicationCode").value("APP-DMS"))
                .andExpect(jsonPath("$.data.list[0].description").value("Document system"))
                .andExpect(jsonPath("$.data.list[0].status").value("ENABLED"));
    }

    @Test
    @WithMockUser
    void createReturnsExpandedApplicationConfigRow() throws Exception {
        given(applicationConfigService.create(any(ApplicationConfigUpsertCommand.class))).willAnswer(invocation -> {
            ApplicationConfigUpsertCommand command = invocation.getArgument(0, ApplicationConfigUpsertCommand.class);
            Assertions.assertEquals("DMS", command.applicationName());
            Assertions.assertEquals("APP-DMS", command.applicationCode());
            Assertions.assertEquals("Document system", command.description());
            Assertions.assertEquals("ENABLED", command.status());
            return new ApplicationConfigRow(5L, "DMS", "APP-DMS", "Document system", "ENABLED", "2026-04-29T10:00:00");
        });

        mockMvc.perform(post("/api/admin/application-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationName": "DMS",
                                  "applicationCode": "APP-DMS",
                                  "description": "Document system",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.applicationName").value("DMS"))
                .andExpect(jsonPath("$.data.applicationCode").value("APP-DMS"))
                .andExpect(jsonPath("$.data.description").value("Document system"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-04-29T10:00:00"));
    }
}
