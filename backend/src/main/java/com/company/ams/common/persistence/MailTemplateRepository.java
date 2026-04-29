package com.company.ams.common.persistence;

import com.company.ams.admin.MailTemplateRow;
import com.company.ams.admin.MailTemplateUpsertCommand;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MailTemplateRepository {
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public MailTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MailTemplateRow> findAll() {
        return jdbcTemplate.query(
                """
                select id, template_name, description, subject, body, status, updated_at
                from admin_mail_template
                where deleted = 0
                order by id
                """,
                (rs, rowNum) -> mapMailTemplateRow(rs));
    }

    public MailTemplateRow create(MailTemplateUpsertCommand command) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    insert into admin_mail_template (
                      template_name,
                      description,
                      subject,
                      body,
                      status,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            statement.setString(1, command.templateName());
            statement.setString(2, command.description());
            statement.setString(3, command.subject());
            statement.setString(4, command.body());
            statement.setString(5, command.status());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create mail template record");
        }
        return getRequiredMailTemplateRow(key.longValue());
    }

    public MailTemplateRow update(long templateId, MailTemplateUpsertCommand command) {
        int updated = jdbcTemplate.update(
                """
                update admin_mail_template
                set template_name = ?,
                    description = ?,
                    subject = ?,
                    body = ?,
                    status = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                command.templateName(),
                command.description(),
                command.subject(),
                command.body(),
                command.status(),
                templateId);
        if (updated == 0) {
            throw new IllegalArgumentException("Mail template " + templateId + " does not exist");
        }
        return getRequiredMailTemplateRow(templateId);
    }

    public void delete(long templateId) {
        int updated = jdbcTemplate.update(
                """
                update admin_mail_template
                set deleted = 1,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                templateId);
        if (updated == 0) {
            throw new IllegalArgumentException("Mail template " + templateId + " does not exist");
        }
    }

    private MailTemplateRow getRequiredMailTemplateRow(long templateId) {
        List<MailTemplateRow> rows = jdbcTemplate.query(
                """
                select id, template_name, description, subject, body, status, updated_at
                from admin_mail_template
                where id = ? and deleted = 0
                """,
                (rs, rowNum) -> mapMailTemplateRow(rs),
                templateId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Mail template " + templateId + " does not exist");
        }
        return rows.get(0);
    }

    private MailTemplateRow mapMailTemplateRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new MailTemplateRow(
                rs.getLong("id"),
                rs.getString("template_name"),
                rs.getString("description"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getString("status"),
                updatedAt == null ? null : updatedAt.toLocalDateTime().format(UPDATED_AT_FORMATTER));
    }
}
