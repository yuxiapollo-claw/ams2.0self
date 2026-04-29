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
        "spring.datasource.url=jdbc:h2:mem:application-config-crud;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplicationConfigCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createUpdateAndDeletePersistApplicationConfig() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mockMvc.perform(post("/api/admin/application-configs")
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "applicationName": "LIMS",
                                  "applicationCode": "APP-LIMS",
                                  "description": "Lab system",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationName").value("LIMS"))
                .andExpect(jsonPath("$.data.applicationCode").value("APP-LIMS"))
                .andExpect(jsonPath("$.data.description").value("Lab system"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        Long configId = jdbcTemplate.queryForObject(
                "select id from admin_application_config where application_code = ?",
                Long.class,
                "APP-LIMS");

        mockMvc.perform(put("/api/admin/application-configs/" + configId)
                        .session(session)
                        .contentType("application/json")
                        .content("""
                                {
                                  "applicationName": "LIMS 2",
                                  "applicationCode": "APP-LIMS-2",
                                  "description": "Lab system updated",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(configId))
                .andExpect(jsonPath("$.data.applicationName").value("LIMS 2"))
                .andExpect(jsonPath("$.data.applicationCode").value("APP-LIMS-2"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                """
                select application_name, application_code, description, status, deleted
                from admin_application_config
                where id = ?
                """,
                configId);

        org.junit.jupiter.api.Assertions.assertEquals("LIMS 2", persisted.get("application_name"));
        org.junit.jupiter.api.Assertions.assertEquals("APP-LIMS-2", persisted.get("application_code"));
        org.junit.jupiter.api.Assertions.assertEquals("Lab system updated", persisted.get("description"));
        org.junit.jupiter.api.Assertions.assertEquals("DISABLED", persisted.get("status"));
        org.junit.jupiter.api.Assertions.assertEquals(0, ((Number) persisted.get("deleted")).intValue());

        mockMvc.perform(delete("/api/admin/application-configs/" + configId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from admin_application_config where id = ?",
                Integer.class,
                configId);
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
