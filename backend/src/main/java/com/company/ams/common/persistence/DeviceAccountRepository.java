package com.company.ams.common.persistence;

import com.company.ams.account.DeviceAccountRow;
import com.company.ams.account.DeviceAccountUpsertCommand;
import com.company.ams.common.api.BusinessException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DeviceAccountRepository {
    private static final String DEVICE_ACCOUNT_SELECT = """
            select da.id,
                   da.device_node_id,
                   device.node_name as device_name,
                   da.user_id,
                   u.user_name,
                   da.account_name,
                   da.account_status,
                   da.source_type,
                   da.remark,
                   role_node.node_name as role_name
            from device_account da
            join asset_node device on device.id = da.device_node_id and device.deleted = 0
            left join sys_user u on u.id = da.user_id and u.deleted = 0
            left join device_account_role dar
                on dar.device_account_id = da.id and dar.relation_status = 'ACTIVE'
            left join asset_node role_node
                on role_node.id = dar.role_node_id and role_node.deleted = 0
            where da.deleted = 0
            """;

    private final JdbcTemplate jdbcTemplate;

    public DeviceAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DeviceAccountRow> findAll() {
        return queryDeviceAccounts("", new Object[0]);
    }

    public List<DeviceAccountRow> findByDeviceNodeId(long deviceNodeId) {
        return queryDeviceAccounts(" and da.device_node_id = ?", deviceNodeId);
    }

    public DeviceAccountRow create(DeviceAccountUpsertCommand command) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into device_account (
                      user_id,
                      device_node_id,
                      account_name,
                      account_status,
                      source_type,
                      remark,
                      created_at,
                      updated_at,
                      deleted
                    ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, 0)
                    """,
                    new String[] {"id"});
            setNullableLong(statement, 1, command.userId());
            statement.setLong(2, command.deviceNodeId());
            statement.setString(3, command.accountName());
            statement.setString(4, command.accountStatus());
            statement.setString(5, command.sourceType());
            statement.setString(6, command.remark());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create device account record");
        }
        return getRequiredRow(key.longValue());
    }

    public DeviceAccountRow update(long deviceAccountId, DeviceAccountUpsertCommand command) {
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    update device_account
                    set user_id = ?,
                        device_node_id = ?,
                        account_name = ?,
                        account_status = ?,
                        source_type = ?,
                        remark = ?,
                        updated_at = current_timestamp
                    where id = ? and deleted = 0
                    """);
            setNullableLong(statement, 1, command.userId());
            statement.setLong(2, command.deviceNodeId());
            statement.setString(3, command.accountName());
            statement.setString(4, command.accountStatus());
            statement.setString(5, command.sourceType());
            statement.setString(6, command.remark());
            statement.setLong(7, deviceAccountId);
            return statement;
        });
        if (updated == 0) {
            throw new IllegalArgumentException("Device account " + deviceAccountId + " does not exist");
        }
        return getRequiredRow(deviceAccountId);
    }

    public void delete(long deviceAccountId) {
        DeviceAccountDeleteTarget target = findDeleteTarget(deviceAccountId);
        if (target == null) {
            throw new IllegalArgumentException("Device account " + deviceAccountId + " does not exist");
        }

        while (true) {
            String tombstoneName = allocateDeletedAccountName(
                    target.deviceNodeId(),
                    target.accountName(),
                    deviceAccountId);
            try {
                int updated = jdbcTemplate.update(
                        """
                        update device_account
                        set deleted = 1,
                            account_name = ?,
                            updated_at = current_timestamp
                        where id = ? and deleted = 0
                        """,
                        tombstoneName,
                        deviceAccountId);
                if (updated == 0) {
                    throw new IllegalArgumentException("Device account " + deviceAccountId + " does not exist");
                }
                return;
            } catch (DuplicateKeyException exception) {
                target = findDeleteTarget(deviceAccountId);
                if (target == null) {
                    throw new IllegalArgumentException("Device account " + deviceAccountId + " does not exist");
                }
            }
        }
    }

    public boolean existsByDeviceAndAccountName(long deviceNodeId, String accountName, Long excludeDeviceAccountId) {
        String sql = """
                select count(*)
                from device_account
                where deleted = 0
                  and device_node_id = ?
                  and account_name = ?
                """;
        Object[] args;
        if (excludeDeviceAccountId == null) {
            args = new Object[] {deviceNodeId, accountName};
        } else {
            sql += " and id <> ?";
            args = new Object[] {deviceNodeId, accountName, excludeDeviceAccountId};
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    public boolean existsActiveDeviceNode(long deviceNodeId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from asset_node
                where id = ? and deleted = 0 and node_type = 'DEVICE'
                """,
                Integer.class,
                deviceNodeId);
        return count != null && count > 0;
    }

    public boolean existsActiveUser(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_user
                where id = ? and deleted = 0
                """,
                Integer.class,
                userId);
        return count != null && count > 0;
    }

    public List<DevicePermissionEntry> findPermissionEntries(long deviceNodeId) {
        return jdbcTemplate.query(
                """
                select role_node.node_name as role_name,
                       u.user_name,
                       da.account_name
                from device_account_role dar
                join device_account da on da.id = dar.device_account_id and da.deleted = 0
                left join sys_user u on u.id = da.user_id and u.deleted = 0
                join asset_node role_node on role_node.id = dar.role_node_id and role_node.deleted = 0
                where da.device_node_id = ?
                  and dar.relation_status = 'ACTIVE'
                order by role_node.sort_no, role_node.id, da.id
                """,
                (rs, rowNum) -> new DevicePermissionEntry(
                        rs.getString("role_name"),
                        rs.getString("user_name"),
                        rs.getString("account_name")),
                deviceNodeId);
    }

    public Optional<RequestTargetAccountRecord> findRequestTargetAccount(long deviceNodeId, String accountName) {
        List<RequestTargetAccountRecord> matches = jdbcTemplate.query(
                """
                select id, user_id
                from device_account
                where deleted = 0
                  and device_node_id = ?
                  and account_name = ?
                """,
                (rs, rowNum) -> new RequestTargetAccountRecord(
                        rs.getLong("id"),
                        rs.getObject("user_id", Long.class)),
                deviceNodeId,
                accountName);
        return matches.stream().findFirst();
    }

    @Transactional
    public void bindRoleToRequestTarget(
            long requestId,
            long targetUserId,
            long targetDeviceNodeId,
            String targetAccountName,
            long roleNodeId) {
        RequestTargetAccountRecord targetAccount =
                requireBindableRequestTargetAccount(targetUserId, targetDeviceNodeId, targetAccountName);
        Timestamp effectiveAt = Timestamp.from(Instant.now());
        if (activateExistingRoleRelation(targetAccount.deviceAccountId(), roleNodeId, effectiveAt, requestId) > 0) {
            return;
        }

        try {
            jdbcTemplate.update(
                    """
                    insert into device_account_role (
                      device_account_id,
                      role_node_id,
                      relation_status,
                      effective_at,
                      source_request_id,
                      created_at,
                      updated_at
                    ) values (?, ?, 'ACTIVE', ?, ?, current_timestamp, current_timestamp)
                    """,
                    targetAccount.deviceAccountId(),
                    roleNodeId,
                    effectiveAt,
                    requestId);
        } catch (DuplicateKeyException ex) {
            if (activateExistingRoleRelation(targetAccount.deviceAccountId(), roleNodeId, effectiveAt, requestId) == 0) {
                throw ex;
            }
        }
    }

    public record DevicePermissionEntry(String roleName, String userName, String accountName) {}

    public record RequestTargetAccountRecord(long deviceAccountId, Long userId) {}

    private record DeviceAccountDeleteTarget(long deviceNodeId, String accountName) {}

    private DeviceAccountRow getRequiredRow(long deviceAccountId) {
        List<DeviceAccountRow> matches = queryDeviceAccounts(" and da.id = ?", deviceAccountId);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Device account " + deviceAccountId + " does not exist");
        }
        return matches.get(0);
    }

    private DeviceAccountDeleteTarget findDeleteTarget(long deviceAccountId) {
        return jdbcTemplate.query(
                """
                select device_node_id, account_name
                from device_account
                where id = ? and deleted = 0
                """,
                rs -> rs.next()
                        ? new DeviceAccountDeleteTarget(
                                rs.getLong("device_node_id"),
                                rs.getString("account_name"))
                        : null,
                deviceAccountId);
    }

    private List<DeviceAccountRow> queryDeviceAccounts(String filterSql, Object... args) {
        List<DeviceAccountRoleJoinRow> rows = jdbcTemplate.query(
                DEVICE_ACCOUNT_SELECT + filterSql + " order by da.id, role_node.sort_no, role_node.id",
                (rs, rowNum) -> new DeviceAccountRoleJoinRow(
                        rs.getLong("id"),
                        rs.getLong("device_node_id"),
                        rs.getString("device_name"),
                        rs.getObject("user_id", Long.class),
                        rs.getString("user_name"),
                        rs.getString("account_name"),
                        rs.getString("account_status"),
                        rs.getString("source_type"),
                        rs.getString("remark"),
                        rs.getString("role_name")),
                args);

        Map<Long, DeviceAccountAccumulator> grouped = new LinkedHashMap<>();
        for (DeviceAccountRoleJoinRow row : rows) {
            DeviceAccountAccumulator accumulator = grouped.computeIfAbsent(
                    row.id(),
                    ignored -> new DeviceAccountAccumulator(
                            row.id(),
                            row.deviceNodeId(),
                            row.deviceName(),
                            row.userId(),
                            row.userName(),
                            row.accountName(),
                            row.accountStatus(),
                            row.sourceType(),
                            row.remark()));
            if (row.roleName() != null && !accumulator.roles.contains(row.roleName())) {
                accumulator.roles.add(row.roleName());
            }
        }

        return grouped.values().stream()
                .map(DeviceAccountAccumulator::toRow)
                .toList();
    }

    private RequestTargetAccountRecord requireBindableRequestTargetAccount(
            long targetUserId,
            long targetDeviceNodeId,
            String targetAccountName) {
        RequestTargetAccountRecord targetAccount = findRequestTargetAccount(targetDeviceNodeId, targetAccountName)
                .orElseThrow(() -> new BusinessException("Target device account does not exist"));

        if (targetAccount.userId() != null && !targetAccount.userId().equals(targetUserId)) {
            throw new BusinessException("Target device account is already bound to a different user");
        }

        if (targetAccount.userId() == null) {
            int updated = jdbcTemplate.update(
                    """
                    update device_account
                    set user_id = ?,
                        updated_at = current_timestamp
                    where id = ? and deleted = 0 and user_id is null
                    """,
                    targetUserId,
                    targetAccount.deviceAccountId());
            if (updated == 0) {
                targetAccount = findRequestTargetAccount(targetDeviceNodeId, targetAccountName)
                        .orElseThrow(() -> new BusinessException("Target device account does not exist"));
                if (targetAccount.userId() == null || !targetAccount.userId().equals(targetUserId)) {
                    throw new BusinessException("Target device account is already bound to a different user");
                }
            } else {
                targetAccount = new RequestTargetAccountRecord(targetAccount.deviceAccountId(), targetUserId);
            }
        }

        return targetAccount;
    }

    private int activateExistingRoleRelation(long deviceAccountId, long roleNodeId, Timestamp effectiveAt, long requestId) {
        return jdbcTemplate.update(
                """
                update device_account_role
                set relation_status = 'ACTIVE',
                    effective_at = coalesce(effective_at, ?),
                    source_request_id = coalesce(source_request_id, ?),
                    updated_at = current_timestamp
                where device_account_id = ? and role_node_id = ?
                """,
                effectiveAt,
                requestId,
                deviceAccountId,
                roleNodeId);
    }

    private String allocateDeletedAccountName(long deviceNodeId, String accountName, long deviceAccountId) {
        long attempt = 0;
        while (true) {
            String candidate = buildDeletedAccountName(accountName, deviceAccountId, attempt);
            Integer count = jdbcTemplate.queryForObject(
                    """
                    select count(*)
                    from device_account
                    where device_node_id = ?
                      and account_name = ?
                      and id <> ?
                    """,
                    Integer.class,
                    deviceNodeId,
                    candidate,
                    deviceAccountId);
            if (count == null || count == 0) {
                return candidate;
            }
            attempt++;
        }
    }

    private String buildDeletedAccountName(String accountName, long deviceAccountId, long attempt) {
        String suffix = "__deleted__" + deviceAccountId + "__" + Long.toString(attempt, 36);
        int prefixLength = Math.max(0, 100 - suffix.length());
        String prefix = accountName == null ? "" : accountName.substring(0, Math.min(accountName.length(), prefixLength));
        return prefix + suffix;
    }

    private void setNullableLong(PreparedStatement statement, int parameterIndex, Long value) throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(parameterIndex, value);
    }

    private record DeviceAccountRoleJoinRow(
            long id,
            long deviceNodeId,
            String deviceName,
            Long userId,
            String userName,
            String accountName,
            String accountStatus,
            String sourceType,
            String remark,
            String roleName) {}

    private static final class DeviceAccountAccumulator {
        private final long id;
        private final long deviceNodeId;
        private final String deviceName;
        private final Long userId;
        private final String userName;
        private final String accountName;
        private final String accountStatus;
        private final String sourceType;
        private final String remark;
        private final List<String> roles = new ArrayList<>();

        private DeviceAccountAccumulator(
                long id,
                long deviceNodeId,
                String deviceName,
                Long userId,
                String userName,
                String accountName,
                String accountStatus,
                String sourceType,
                String remark) {
            this.id = id;
            this.deviceNodeId = deviceNodeId;
            this.deviceName = deviceName;
            this.userId = userId;
            this.userName = userName;
            this.accountName = accountName;
            this.accountStatus = accountStatus;
            this.sourceType = sourceType;
            this.remark = remark;
        }

        private DeviceAccountRow toRow() {
            return new DeviceAccountRow(
                    id,
                    deviceNodeId,
                    deviceName,
                    userId,
                    userName,
                    accountName,
                    accountStatus,
                    sourceType,
                    remark,
                    List.copyOf(roles));
        }
    }
}
