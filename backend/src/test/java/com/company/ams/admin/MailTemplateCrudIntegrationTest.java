package com.company.ams.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        "spring.datasource.url=jdbc:h2:mem:mail-template-crud;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MailTemplateCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createUpdateAndDeletePersistMailTemplate() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/admin/mail-templates")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "templateName": "执行完成通知",
                                  "description": "执行完成提醒",
                                  "subject": "【AMS】执行完成",
                                  "body": "执行完成正文",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("执行完成通知"))
                .andExpect(jsonPath("$.data.subject").value("【AMS】执行完成"));

        Long templateId = jdbcTemplate.queryForObject(
                "select id from admin_mail_template where template_name = ?",
                Long.class,
                "执行完成通知");

        mockMvc.perform(put("/api/admin/mail-templates/" + templateId)
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "templateName": "执行完成通知v2",
                                  "description": "执行完成提醒更新",
                                  "subject": "【AMS】执行完成v2",
                                  "body": "执行完成正文更新",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(templateId))
                .andExpect(jsonPath("$.data.templateName").value("执行完成通知v2"))
                .andExpect(jsonPath("$.data.subject").value("【AMS】执行完成v2"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                """
                select template_name, description, subject, body, status, deleted
                from admin_mail_template
                where id = ?
                """,
                templateId);

        org.junit.jupiter.api.Assertions.assertEquals("执行完成通知v2", persisted.get("template_name"));
        org.junit.jupiter.api.Assertions.assertEquals("执行完成提醒更新", persisted.get("description"));
        org.junit.jupiter.api.Assertions.assertEquals("【AMS】执行完成v2", persisted.get("subject"));
        org.junit.jupiter.api.Assertions.assertEquals("执行完成正文更新", persisted.get("body"));
        org.junit.jupiter.api.Assertions.assertEquals("DISABLED", persisted.get("status"));
        org.junit.jupiter.api.Assertions.assertEquals(0, ((Number) persisted.get("deleted")).intValue());

        mockMvc.perform(delete("/api/admin/mail-templates/" + templateId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from admin_mail_template where id = ?",
                Integer.class,
                templateId);
        org.junit.jupiter.api.Assertions.assertEquals(1, deleted);
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
