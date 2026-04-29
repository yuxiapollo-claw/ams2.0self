package com.company.ams.common.persistence;

import com.company.ams.audit.AuditLogRow;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AuditLogRow> findAll() {
        return jdbcTemplate.query(
                """
                select id, action_type, operator_name, object_type, created_at
                from audit_log
                order by created_at desc, id desc
                """,
                (rs, rowNum) -> new AuditLogRow(
                        rs.getLong("id"),
                        rs.getString("action_type"),
                        rs.getString("operator_name"),
                        rs.getString("object_type"),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString()));
    }

    public void insert(String actionType, String operatorName, String objectType, Long objectId) {
        jdbcTemplate.update(
                """
                insert into audit_log (action_type, operator_name, object_type, object_id, created_at)
                values (?, ?, ?, ?, current_timestamp)
                """,
                actionType,
                operatorName,
                objectType,
                objectId);
    }
}
