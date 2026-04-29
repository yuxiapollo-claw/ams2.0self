package com.company.ams.common.persistence;

import com.company.ams.user.DepartmentRow;
import com.company.ams.user.DepartmentUpsertCommand;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DepartmentRepository {
    private static final String LIVE_MEMBER_FILTER = """
            u.deleted = 0
            """;
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DepartmentRow> findAll() {
        return jdbcTemplate.query(
                """
                select d.id,
                       d.department_name,
                       d.manager_user_id,
                       coalesce(manager.user_name, '') as manager_user_name,
                       d.description,
                       coalesce(member_counts.member_count, 0) as member_count,
                       d.updated_at,
                       d.status
                from sys_department d
                left join sys_user manager on manager.id = d.manager_user_id and manager.deleted = 0
                left join (
                  select u.department_id, count(*) as member_count
                  from sys_user u
                  where %s
                  group by u.department_id
                ) member_counts on member_counts.department_id = d.id
                where d.deleted = 0
                order by d.id
                """.formatted(LIVE_MEMBER_FILTER),
                (rs, rowNum) -> mapDepartmentRow(rs));
    }

    public DepartmentRow create(DepartmentUpsertCommand command) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    insert into sys_department (
                      department_name,
                      manager_user_id,
                      description,
                      status,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            statement.setString(1, command.departmentName());
            if (command.managerUserId() == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, command.managerUserId());
            }
            statement.setString(3, command.description());
            statement.setString(4, command.status());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create department record");
        }
        return getRequiredDepartmentRow(key.longValue());
    }

    public DepartmentRow update(long departmentId, DepartmentUpsertCommand command) {
        int updated = jdbcTemplate.update(
                """
                update sys_department
                set department_name = ?,
                    manager_user_id = ?,
                    description = ?,
                    status = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                ps -> {
                    ps.setString(1, command.departmentName());
                    if (command.managerUserId() == null) {
                        ps.setNull(2, java.sql.Types.BIGINT);
                    } else {
                        ps.setLong(2, command.managerUserId());
                    }
                    ps.setString(3, command.description());
                    ps.setString(4, command.status());
                    ps.setLong(5, departmentId);
                });
        if (updated == 0) {
            throw new IllegalArgumentException("Department " + departmentId + " does not exist");
        }
        return getRequiredDepartmentRow(departmentId);
    }

    public void delete(long departmentId) {
        int updated = jdbcTemplate.update(
                """
                update sys_department
                set deleted = 1,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                departmentId);
        if (updated == 0) {
            throw new IllegalArgumentException("Department " + departmentId + " does not exist");
        }
    }

    public boolean existsNonDeletedUser(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_user
                where id = ?
                  and deleted = 0
                """,
                Integer.class,
                userId);
        return count != null && count > 0;
    }

    public boolean existsNonDeletedMembers(long departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_user u
                where u.department_id = ?
                  and %s
                """.formatted(LIVE_MEMBER_FILTER),
                Integer.class,
                departmentId);
        return count != null && count > 0;
    }

    private DepartmentRow getRequiredDepartmentRow(long departmentId) {
        List<DepartmentRow> matches = jdbcTemplate.query(
                """
                select d.id,
                       d.department_name,
                       d.manager_user_id,
                       coalesce(manager.user_name, '') as manager_user_name,
                       d.description,
                       coalesce(member_counts.member_count, 0) as member_count,
                       d.updated_at,
                       d.status
                from sys_department d
                left join sys_user manager on manager.id = d.manager_user_id and manager.deleted = 0
                left join (
                  select u.department_id, count(*) as member_count
                  from sys_user u
                  where %s
                  group by u.department_id
                ) member_counts on member_counts.department_id = d.id
                where d.deleted = 0 and d.id = ?
                """.formatted(LIVE_MEMBER_FILTER),
                (rs, rowNum) -> mapDepartmentRow(rs),
                departmentId);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Department " + departmentId + " does not exist");
        }
        return matches.get(0);
    }

    private DepartmentRow mapDepartmentRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new DepartmentRow(
                rs.getLong("id"),
                rs.getString("department_name"),
                rs.getObject("manager_user_id", Long.class),
                rs.getString("manager_user_name"),
                rs.getString("description"),
                rs.getInt("member_count"),
                rs.getString("status"),
                updatedAt == null ? null : updatedAt.toLocalDateTime().format(UPDATED_AT_FORMATTER));
    }
}
