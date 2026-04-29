package com.company.ams.common.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long userTotal() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from sys_user where deleted = 0",
                Long.class);
        return count == null ? 0 : count;
    }

    public long departmentTotal() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from sys_department where deleted = 0",
                Long.class);
        return count == null ? 0 : count;
    }

    public long deviceAccountTotal() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from device_account where deleted = 0",
                Long.class);
        return count == null ? 0 : count;
    }

    public long pendingRequestTotal() {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from request_order
                where current_status <> 'COMPLETED'
                """,
                Long.class);
        return count == null ? 0 : count;
    }

    public List<AlertRow> findAlerts() {
        List<AlertRow> result = new ArrayList<>();

        // "Unbound accounts" is defined as device_account.user_id IS NULL.
        // In the current schema this column is NOT NULL, so we derive 0 via schema inspection
        // (instead of running a dead query or hardcoding a fake constant).
        long unboundCount = unboundAccountTotal();
        result.add(new AlertRow(
                "unbound-accounts",
                "Unbound Accounts",
                "Device accounts without a bound user",
                unboundCount));

        Integer noActiveRoleAccounts = jdbcTemplate.query(
                """
                select count(*) as missing_cnt
                from (
                  select da.id
                  from device_account da
                  left join device_account_role dar
                    on dar.device_account_id = da.id and dar.relation_status = 'ACTIVE'
                  where da.deleted = 0
                  group by da.id
                  having count(dar.id) = 0
                ) t
                """,
                rs -> rs.next() ? rs.getInt("missing_cnt") : 0);
        if (noActiveRoleAccounts != null && noActiveRoleAccounts > 0) {
            result.add(new AlertRow(
                    "no-active-roles",
                    "Accounts Missing Active Roles",
                    "Device accounts that have no ACTIVE role bindings",
                    noActiveRoleAccounts));
        }

        return List.copyOf(result);
    }

    private long unboundAccountTotal() {
        if (!isColumnNullable("device_account", "user_id")) {
            return 0;
        }
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from device_account where deleted = 0 and user_id is null",
                Long.class);
        return count == null ? 0 : count;
    }

    private boolean isColumnNullable(String tableName, String columnName) {
        if (jdbcTemplate.getDataSource() == null) {
            // Conservative: if we cannot inspect schema, fall back to querying.
            return true;
        }

        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String connectionSchema = safeSchema(connection);
            String[] schemas = new String[] {connectionSchema, "PUBLIC", null};
            List<String> tableCandidates = List.of(tableName, tableName.toUpperCase(), tableName.toLowerCase());
            List<String> columnCandidates = List.of(columnName, columnName.toUpperCase(), columnName.toLowerCase());

            for (String schema : schemas) {
                for (String table : tableCandidates) {
                    for (String column : columnCandidates) {
                        try (ResultSet rs = metaData.getColumns(null, schema, table, column)) {
                            if (rs.next()) {
                                int nullableFlag = rs.getInt("NULLABLE");
                                return nullableFlag == DatabaseMetaData.columnNullable;
                            }
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // Conservative: if schema inspection fails, fall back to querying.
            return true;
        }

        // Column not found: treat as nullable to avoid suppressing legitimate data.
        return true;
    }

    private String safeSchema(Connection connection) {
        try {
            return connection.getSchema();
        } catch (SQLException ignored) {
            return null;
        }
    }

    public List<RecentRequestRow> findRecentRequests(int limit) {
        return jdbcTemplate.query(
                """
                select id, request_no, request_type, current_status, created_at
                from request_order
                order by created_at desc, id desc
                limit ?
                """,
                (rs, rowNum) -> {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    Instant createdAtInstant = createdAt == null ? null : createdAt.toInstant();
                    return new RecentRequestRow(
                            rs.getLong("id"),
                            rs.getString("request_no"),
                            rs.getString("request_type"),
                            rs.getString("current_status"),
                            createdAtInstant);
                },
                limit);
    }

    public record AlertRow(
            String alertKey,
            String title,
            String description,
            long count) {}

    public record RecentRequestRow(
            long id,
            String requestNo,
            String requestType,
            String currentStatus,
            Instant createdAt) {}
}
