package com.company.ams.common.persistence;

import com.company.ams.access.PermissionRequestRow;
import java.sql.PreparedStatement;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccessRequestRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccessRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PermissionRequestRow createRequest(
            long applicantUserId,
            long applicantDepartmentId,
            long targetUserId,
            long permissionId,
            String permissionPath,
            String requestType,
            String reason,
            String currentStatus,
            String currentStep,
            Long departmentLeaderUserId,
            String departmentLeaderUserName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String temporaryRequestNo = "TMP-" + System.nanoTime();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into access_request (
                      request_no,
                      request_type,
                      applicant_user_id,
                      applicant_department_id,
                      target_user_id,
                      permission_id,
                      permission_path_snapshot,
                      request_reason,
                      current_status,
                      current_step,
                      department_leader_snapshot_id,
                      department_leader_snapshot_name,
                      submitted_at,
                      created_at,
                      updated_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                    """,
                    new String[] {"id"});
            statement.setString(1, temporaryRequestNo);
            statement.setString(2, requestType);
            statement.setLong(3, applicantUserId);
            statement.setLong(4, applicantDepartmentId);
            statement.setLong(5, targetUserId);
            statement.setLong(6, permissionId);
            statement.setString(7, permissionPath);
            statement.setString(8, reason);
            statement.setString(9, currentStatus);
            statement.setString(10, currentStep);
            if (departmentLeaderUserId == null) {
                statement.setNull(11, java.sql.Types.BIGINT);
            } else {
                statement.setLong(11, departmentLeaderUserId);
            }
            statement.setString(12, departmentLeaderUserName);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create request record");
        }
        long requestId = key.longValue();
        String requestNo = "ARQ" + requestId;
        jdbcTemplate.update(
                """
                update access_request
                set request_no = ?,
                    updated_at = current_timestamp
                where id = ?
                """,
                requestNo,
                requestId);
        return getRequiredRequest(requestId);
    }

    public List<PermissionRequestRow> listRequests(long userId, boolean systemAdmin) {
        String sql = """
                select id,
                       request_no,
                       request_type,
                       permission_path_snapshot,
                       current_status,
                       request_reason,
                       created_at
                from access_request
                %s
                order by created_at desc, id desc
                """.formatted(systemAdmin ? "" : "where applicant_user_id = ?");
        if (systemAdmin) {
            return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
        }
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    public boolean existsOpenRequest(long targetUserId, long permissionId, String requestType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from access_request
                where target_user_id = ?
                  and permission_id = ?
                  and request_type = ?
                  and current_status <> 'COMPLETED'
                """,
                Integer.class,
                targetUserId,
                permissionId,
                requestType);
        return count != null && count > 0;
    }

    public DepartmentLeaderSnapshot findDepartmentLeaderSnapshot(long departmentId) {
        return jdbcTemplate.query(
                """
                select d.manager_user_id,
                       manager.user_name as manager_user_name
                from sys_department d
                left join sys_user manager on manager.id = d.manager_user_id and manager.deleted = 0
                where d.id = ? and d.deleted = 0
                """,
                rs -> rs.next()
                        ? new DepartmentLeaderSnapshot(
                                (Long) rs.getObject("manager_user_id"),
                                rs.getString("manager_user_name"))
                        : new DepartmentLeaderSnapshot(null, null),
                departmentId);
    }

    private PermissionRequestRow getRequiredRequest(long requestId) {
        PermissionRequestRow row = jdbcTemplate.query(
                """
                select id,
                       request_no,
                       request_type,
                       permission_path_snapshot,
                       current_status,
                       request_reason,
                       created_at
                from access_request
                where id = ?
                """,
                rs -> rs.next() ? mapRow(rs) : null,
                requestId);
        if (row == null) {
            throw new IllegalArgumentException("Request " + requestId + " does not exist");
        }
        return row;
    }

    private PermissionRequestRow mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PermissionRequestRow(
                rs.getLong("id"),
                rs.getString("request_no"),
                rs.getString("request_type"),
                rs.getString("permission_path_snapshot"),
                rs.getString("current_status"),
                rs.getString("request_reason"),
                rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString());
    }

    public record DepartmentLeaderSnapshot(Long leaderUserId, String leaderUserName) {}
}
