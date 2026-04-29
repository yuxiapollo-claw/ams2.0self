package com.company.ams.common.persistence;

import com.company.ams.admin.ApplicationConfigRow;
import com.company.ams.admin.ApplicationConfigUpsertCommand;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationConfigRepository {
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public ApplicationConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApplicationConfigRow> findAll() {
        return jdbcTemplate.query(
                """
                select id, application_name, application_code, description, status, updated_at
                from admin_application_config
                where deleted = 0
                order by id
                """,
                (rs, rowNum) -> mapApplicationConfigRow(rs));
    }

    public ApplicationConfigRow create(ApplicationConfigUpsertCommand command) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    insert into admin_application_config (
                      application_name,
                      application_code,
                      description,
                      status,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            statement.setString(1, command.applicationName());
            statement.setString(2, command.applicationCode());
            statement.setString(3, command.description());
            statement.setString(4, command.status());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create application config record");
        }
        return getRequiredApplicationConfigRow(key.longValue());
    }

    public ApplicationConfigRow update(long configId, ApplicationConfigUpsertCommand command) {
        int updated = jdbcTemplate.update(
                """
                update admin_application_config
                set application_name = ?,
                    application_code = ?,
                    description = ?,
                    status = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                command.applicationName(),
                command.applicationCode(),
                command.description(),
                command.status(),
                configId);
        if (updated == 0) {
            throw new IllegalArgumentException("Application config " + configId + " does not exist");
        }
        return getRequiredApplicationConfigRow(configId);
    }

    public void delete(long configId) {
        int updated = jdbcTemplate.update(
                """
                update admin_application_config
                set deleted = 1,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                configId);
        if (updated == 0) {
            throw new IllegalArgumentException("Application config " + configId + " does not exist");
        }
    }

    private ApplicationConfigRow getRequiredApplicationConfigRow(long configId) {
        List<ApplicationConfigRow> rows = jdbcTemplate.query(
                """
                select id, application_name, application_code, description, status, updated_at
                from admin_application_config
                where id = ? and deleted = 0
                """,
                (rs, rowNum) -> mapApplicationConfigRow(rs),
                configId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Application config " + configId + " does not exist");
        }
        return rows.get(0);
    }

    private ApplicationConfigRow mapApplicationConfigRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new ApplicationConfigRow(
                rs.getLong("id"),
                rs.getString("application_name"),
                rs.getString("application_code"),
                rs.getString("description"),
                rs.getString("status"),
                updatedAt == null ? null : updatedAt.toLocalDateTime().format(UPDATED_AT_FORMATTER));
    }
}
