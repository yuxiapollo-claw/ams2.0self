package com.company.ams.common.persistence;

import com.company.ams.user.UserRow;
import com.company.ams.user.UserUpsertCommand;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private static final String DEFAULT_PASSWORD_HASH =
            "$2a$10$mMQ4LA/fmGkbcXoS0ZITM.QOjGsgIo9ECYRHi2ucTBOdmI/BvCyPq";

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthUserRecord> findByLoginName(String loginName) {
        List<AuthUserRecord> matches = jdbcTemplate.query(
                """
                select id, login_name, user_name, department_id, password_hash, account_status
                from sys_user
                where deleted = 0 and login_name = ?
                """,
                (rs, rowNum) -> new AuthUserRecord(
                        rs.getLong("id"),
                        rs.getString("login_name"),
                        rs.getString("user_name"),
                        rs.getLong("department_id"),
                        rs.getString("password_hash"),
                        rs.getString("account_status")),
                loginName);
        return matches.stream().findFirst();
    }

    public Optional<RequestUserRecord> findRequestUserById(long userId) {
        List<RequestUserRecord> matches = jdbcTemplate.query(
                """
                select id, user_name, department_id
                from sys_user
                where deleted = 0 and id = ?
                """,
                (rs, rowNum) -> new RequestUserRecord(
                        rs.getLong("id"),
                        rs.getString("user_name"),
                        rs.getLong("department_id")),
                userId);
        return matches.stream().findFirst();
    }

    public List<UserRow> findAll() {
        return jdbcTemplate.query(
                """
                select u.id,
                       u.user_code,
                       u.user_name,
                       u.department_id,
                       d.department_name,
                       u.employment_status,
                       u.login_name,
                       u.account_status
                from sys_user u
                join sys_department d on d.id = u.department_id
                where u.deleted = 0 and d.deleted = 0
                order by u.id
                """,
                (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("user_code"),
                        rs.getString("user_name"),
                        rs.getLong("department_id"),
                        rs.getString("department_name"),
                        rs.getString("employment_status"),
                        rs.getString("login_name"),
                        rs.getString("account_status")));
    }

    public UserRow create(UserUpsertCommand command) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    insert into sys_user (
                      user_code,
                      user_name,
                      department_id,
                      employment_status,
                      login_name,
                      password_hash,
                      account_status,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            statement.setString(1, command.userCode());
            statement.setString(2, command.userName());
            statement.setLong(3, command.departmentId());
            statement.setString(4, command.employmentStatus());
            statement.setString(5, command.loginName());
            statement.setString(6, DEFAULT_PASSWORD_HASH);
            statement.setString(7, command.accountStatus());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create user record");
        }
        return getRequiredUserRow(key.longValue());
    }

    public UserRow update(long userId, UserUpsertCommand command) {
        int updated = jdbcTemplate.update(
                """
                update sys_user
                set user_code = ?,
                    user_name = ?,
                    department_id = ?,
                    employment_status = ?,
                    login_name = ?,
                    account_status = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                command.userCode(),
                command.userName(),
                command.departmentId(),
                command.employmentStatus(),
                command.loginName(),
                command.accountStatus(),
                userId);
        if (updated == 0) {
            throw new IllegalArgumentException("User " + userId + " does not exist");
        }
        return getRequiredUserRow(userId);
    }

    public UserRow updateStatus(long userId, String accountStatus) {
        int updated = jdbcTemplate.update(
                """
                update sys_user
                set account_status = ?,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                accountStatus,
                userId);
        if (updated == 0) {
            throw new IllegalArgumentException("User " + userId + " does not exist");
        }
        return getRequiredUserRow(userId);
    }

    public void delete(long userId) {
        int updated = jdbcTemplate.update(
                """
                update sys_user
                set deleted = 1,
                    updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                userId);
        if (updated == 0) {
            throw new IllegalArgumentException("User " + userId + " does not exist");
        }
    }

    public boolean existsUserCode(String userCode, Long excludeUserId) {
        String sql = """
                select count(*)
                from sys_user
                where deleted = 0 and user_code = ?
                """;
        Object[] args;
        if (excludeUserId == null) {
            args = new Object[] {userCode};
        } else {
            sql += " and id <> ?";
            args = new Object[] {userCode, excludeUserId};
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    public boolean existsLoginName(String loginName, Long excludeUserId) {
        String sql = """
                select count(*)
                from sys_user
                where deleted = 0 and login_name = ?
                """;
        Object[] args;
        if (excludeUserId == null) {
            args = new Object[] {loginName};
        } else {
            sql += " and id <> ?";
            args = new Object[] {loginName, excludeUserId};
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    public boolean existsDeviceAccountsOwnedByUser(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from device_account
                where deleted = 0 and user_id = ?
                """,
                Integer.class,
                userId);
        return count != null && count > 0;
    }

    public boolean existsUnfinishedRequestsByUser(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from request_order
                where (applicant_user_id = ? or target_user_id = ?)
                  and current_status <> 'COMPLETED'
                """,
                Integer.class,
                userId,
                userId);
        return count != null && count > 0;
    }

    public boolean existsActiveDepartment(long departmentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_department
                where deleted = 0 and id = ?
                """,
                Integer.class,
                departmentId);
        return count != null && count > 0;
    }

    private UserRow getRequiredUserRow(long userId) {
        List<UserRow> matches = jdbcTemplate.query(
                """
                select u.id,
                       u.user_code,
                       u.user_name,
                       u.department_id,
                       d.department_name,
                       u.employment_status,
                       u.login_name,
                       u.account_status
                from sys_user u
                join sys_department d on d.id = u.department_id
                where u.deleted = 0 and d.deleted = 0 and u.id = ?
                """,
                (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("user_code"),
                        rs.getString("user_name"),
                        rs.getLong("department_id"),
                        rs.getString("department_name"),
                        rs.getString("employment_status"),
                        rs.getString("login_name"),
                        rs.getString("account_status")),
                userId);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("User " + userId + " does not exist");
        }
        return matches.get(0);
    }

    public record AuthUserRecord(
            long id,
            String loginName,
            String userName,
            long departmentId,
            String passwordHash,
            String accountStatus) {}

    public record RequestUserRecord(
            long id,
            String userName,
            long departmentId) {}
}
