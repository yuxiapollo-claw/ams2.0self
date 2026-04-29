package com.company.ams.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-controller;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginName":"admin","password":"admin123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.token").value("session-authenticated"))
            .andExpect(jsonPath("$.data.user.loginName").value("admin"))
            .andReturn();

        Object contextAttribute = result.getRequest()
                .getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SecurityContext securityContext = assertInstanceOf(SecurityContext.class, contextAttribute);
        Authentication authentication = securityContext.getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, authentication.getPrincipal());
        assertEquals("admin", principal.loginName());
    }

    @Test
    void loginUsesHashedPasswordStoredInSeedData() throws Exception {
        String storedPasswordHash = jdbcTemplate.queryForObject(
                "select password_hash from sys_user where login_name = ?",
                String.class,
                "admin");

        assertNotEquals("admin123", storedPasswordHash);
        assertTrue(storedPasswordHash.startsWith("$2"));

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginName":"admin","password":"admin123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.loginName").value("admin"));
    }

    @Test
    void loginWithBadCredentialsReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginName":"admin","password":"wrong"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    @Test
    void loginWithoutCsrfSucceedsForSpaClients() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginName":"admin","password":"admin123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.loginName").value("admin"));
    }
}
