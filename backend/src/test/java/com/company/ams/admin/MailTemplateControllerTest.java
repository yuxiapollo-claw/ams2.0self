package com.company.ams.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ams.auth.SecurityConfig;
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

@WebMvcTest(MailTemplateController.class)
@Import(SecurityConfig.class)
class MailTemplateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MailTemplateService mailTemplateService;

    @Test
    @WithMockUser
    void listReturnsMailTemplateRows() throws Exception {
        given(mailTemplateService.list()).willReturn(List.of(
                new MailTemplateRow(1L, "申请创建通知", "新申请通知", "【AMS】新申请", "body", "ENABLED", "2026-04-29T10:00:00")));

        mockMvc.perform(get("/api/admin/mail-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].templateName").value("申请创建通知"))
                .andExpect(jsonPath("$.data.list[0].description").value("新申请通知"))
                .andExpect(jsonPath("$.data.list[0].subject").value("【AMS】新申请"))
                .andExpect(jsonPath("$.data.list[0].body").value("body"));
    }

    @Test
    @WithMockUser
    void createReturnsExpandedMailTemplateRow() throws Exception {
        given(mailTemplateService.create(any(MailTemplateUpsertCommand.class))).willAnswer(invocation -> {
            MailTemplateUpsertCommand command = invocation.getArgument(0, MailTemplateUpsertCommand.class);
            Assertions.assertEquals("申请创建通知", command.templateName());
            Assertions.assertEquals("新申请通知", command.description());
            Assertions.assertEquals("【AMS】新申请", command.subject());
            Assertions.assertEquals("body", command.body());
            Assertions.assertEquals("ENABLED", command.status());
            return new MailTemplateRow(6L, "申请创建通知", "新申请通知", "【AMS】新申请", "body", "ENABLED", "2026-04-29T10:00:00");
        });

        mockMvc.perform(post("/api/admin/mail-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "申请创建通知",
                                  "description": "新申请通知",
                                  "subject": "【AMS】新申请",
                                  "body": "body",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(6))
                .andExpect(jsonPath("$.data.templateName").value("申请创建通知"))
                .andExpect(jsonPath("$.data.description").value("新申请通知"))
                .andExpect(jsonPath("$.data.subject").value("【AMS】新申请"))
                .andExpect(jsonPath("$.data.body").value("body"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }
}
