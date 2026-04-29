package com.company.ams.common.persistence;

import com.company.ams.access.AccessSystemRow;
import com.company.ams.access.AccessSystemUpsertCommand;
import com.company.ams.access.PermissionNodeUpsertCommand;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccessCatalogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccessCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AccessSystemRow> findSystems() {
        return jdbcTemplate.query(
                """
                select id, system_name, system_description
                from access_system
                where deleted = 0
                order by system_name, id
                """,
                (rs, rowNum) -> new AccessSystemRow(
                        rs.getLong("id"),
                        rs.getString("system_name"),
                        rs.getString("system_description")));
    }

    public AccessSystemRow createSystem(AccessSystemUpsertCommand command) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    insert into access_system (
                      system_name,
                      system_description,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            statement.setString(1, command.systemName());
            statement.setString(2, command.systemDescription());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create system record");
        }
        return getRequiredSystem(key.longValue());
    }

    public AccessSystemRow updateSystem(long systemId, AccessSystemUpsertCommand command) {
        int updated = jdbcTemplate.update(
                """
                update access_system
                set system_name = ?,
                    system_description = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                command.systemName(),
                command.systemDescription(),
                systemId);
        if (updated == 0) {
            throw new IllegalArgumentException("System " + systemId + " does not exist");
        }
        return getRequiredSystem(systemId);
    }

    public void deleteSystem(long systemId) {
        int updated = jdbcTemplate.update(
                """
                update access_system
                set deleted = 1,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                systemId);
        if (updated == 0) {
            throw new IllegalArgumentException("System " + systemId + " does not exist");
        }
    }

    public boolean existsSystemName(String systemName, Long excludeSystemId) {
        String sql = """
                select count(*)
                from access_system
                where deleted = 0 and system_name = ?
                """;
        Object[] args;
        if (excludeSystemId == null) {
            args = new Object[] {systemName};
        } else {
            sql += " and id <> ?";
            args = new Object[] {systemName, excludeSystemId};
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    public boolean hasPermissionsUnderSystem(long systemId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from access_permission
                where system_id = ? and deleted = 0
                """,
                Integer.class,
                systemId);
        return count != null && count > 0;
    }

    public SystemRecord getRequiredSystemRecord(long systemId) {
        SystemRecord record = jdbcTemplate.query(
                """
                select id, system_name, system_description
                from access_system
                where id = ? and deleted = 0
                """,
                rs -> rs.next()
                        ? new SystemRecord(
                                rs.getLong("id"),
                                rs.getString("system_name"),
                                rs.getString("system_description"))
                        : null,
                systemId);
        if (record == null) {
            throw new IllegalArgumentException("System " + systemId + " does not exist");
        }
        return record;
    }

    public List<PermissionRecord> findPermissions() {
        return jdbcTemplate.query(
                """
                select id,
                       system_id,
                       parent_permission_id,
                       permission_name,
                       full_path,
                       level_num,
                       enabled
                from access_permission
                where deleted = 0
                order by level_num, id
                """,
                (rs, rowNum) -> new PermissionRecord(
                        rs.getLong("id"),
                        rs.getLong("system_id"),
                        (Long) rs.getObject("parent_permission_id"),
                        rs.getString("permission_name"),
                        rs.getString("full_path"),
                        rs.getInt("level_num"),
                        rs.getBoolean("enabled")));
    }

    public PermissionRecord getRequiredPermissionRecord(long permissionId) {
        PermissionRecord record = jdbcTemplate.query(
                """
                select id,
                       system_id,
                       parent_permission_id,
                       permission_name,
                       full_path,
                       level_num,
                       enabled
                from access_permission
                where id = ? and deleted = 0
                """,
                rs -> rs.next()
                        ? new PermissionRecord(
                                rs.getLong("id"),
                                rs.getLong("system_id"),
                                (Long) rs.getObject("parent_permission_id"),
                                rs.getString("permission_name"),
                                rs.getString("full_path"),
                                rs.getInt("level_num"),
                                rs.getBoolean("enabled"))
                        : null,
                permissionId);
        if (record == null) {
            throw new IllegalArgumentException("Permission " + permissionId + " does not exist");
        }
        return record;
    }

    public PermissionRecord createPermission(PermissionNodeUpsertCommand command, String fullPath, int level) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    insert into access_permission (
                      system_id,
                      parent_permission_id,
                      permission_name,
                      full_path,
                      level_num,
                      enabled,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            statement.setLong(1, command.systemId());
            if (command.parentPermissionId() == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, command.parentPermissionId());
            }
            statement.setString(3, command.permissionName());
            statement.setString(4, fullPath);
            statement.setInt(5, level);
            statement.setBoolean(6, Boolean.TRUE.equals(command.enabled()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create permission record");
        }
        return getRequiredPermissionRecord(key.longValue());
    }

    public PermissionRecord updatePermission(
            long permissionId,
            PermissionNodeUpsertCommand command,
            String fullPath,
            int level) {
        int updated = jdbcTemplate.update(
                """
                update access_permission
                set permission_name = ?,
                    full_path = ?,
                    level_num = ?,
                    enabled = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                command.permissionName(),
                fullPath,
                level,
                Boolean.TRUE.equals(command.enabled()),
                permissionId);
        if (updated == 0) {
            throw new IllegalArgumentException("Permission " + permissionId + " does not exist");
        }
        return getRequiredPermissionRecord(permissionId);
    }

    public void updatePermissionPath(long permissionId, String fullPath, int level) {
        jdbcTemplate.update(
                """
                update access_permission
                set full_path = ?,
                    level_num = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                fullPath,
                level,
                permissionId);
    }

    public void deletePermission(long permissionId) {
        int updated = jdbcTemplate.update(
                """
                update access_permission
                set deleted = 1,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                permissionId);
        if (updated == 0) {
            throw new IllegalArgumentException("Permission " + permissionId + " does not exist");
        }
    }

    public boolean existsPermissionName(long systemId, Long parentPermissionId, String permissionName, Long excludePermissionId) {
        StringBuilder sql = new StringBuilder(
                """
                select count(*)
                from access_permission
                where deleted = 0
                  and system_id = ?
                  and permission_name = ?
                """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        args.add(systemId);
        args.add(permissionName);
        if (parentPermissionId == null) {
            sql.append(" and parent_permission_id is null");
        } else {
            sql.append(" and parent_permission_id = ?");
            args.add(parentPermissionId);
        }
        if (excludePermissionId != null) {
            sql.append(" and id <> ?");
            args.add(excludePermissionId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
    }

    public boolean hasChildPermissions(long permissionId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from access_permission
                where parent_permission_id = ? and deleted = 0
                """,
                Integer.class,
                permissionId);
        return count != null && count > 0;
    }

    public boolean hasAssignments(long permissionId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from user_permission_assignment
                where permission_id = ?
                """,
                Integer.class,
                permissionId);
        return count != null && count > 0;
    }

    public boolean hasRequests(long permissionId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from access_request
                where permission_id = ?
                """,
                Integer.class,
                permissionId);
        return count != null && count > 0;
    }

    public List<Long> findAssignedPermissionIds(long userId) {
        return jdbcTemplate.query(
                """
                select permission_id
                from user_permission_assignment
                where user_id = ?
                order by permission_id
                """,
                (rs, rowNum) -> rs.getLong("permission_id"),
                userId);
    }

    private AccessSystemRow getRequiredSystem(long systemId) {
        AccessSystemRow row = jdbcTemplate.query(
                """
                select id, system_name, system_description
                from access_system
                where id = ? and deleted = 0
                """,
                rs -> rs.next()
                        ? new AccessSystemRow(
                                rs.getLong("id"),
                                rs.getString("system_name"),
                                rs.getString("system_description"))
                        : null,
                systemId);
        if (row == null) {
            throw new IllegalArgumentException("System " + systemId + " does not exist");
        }
        return row;
    }

    public record SystemRecord(long id, String systemName, String systemDescription) {}

    public record PermissionRecord(
            long id,
            long systemId,
            Long parentPermissionId,
            String permissionName,
            String fullPath,
            int level,
            boolean enabled) {}
}
