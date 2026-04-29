package com.company.ams.common.persistence;

import com.company.ams.common.api.BusinessException;
import com.company.ams.request.CreateRequestCommand;
import com.company.ams.request.RequestItemCommand;
import com.company.ams.request.RequestCreateResponse;
import com.company.ams.request.RequestListItem;
import com.company.ams.request.RequestListPayload;
import com.company.ams.request.RequestWorkflowItem;
import com.company.ams.request.RequestWorkflowStore;
import java.sql.PreparedStatement;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RequestWorkflowPersistence implements RequestWorkflowStore {
    private static final String ITEM_PENDING = "PENDING";
    private static final String ITEM_EXECUTED = "EXECUTED";

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAccountRepository deviceAccountRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    public RequestWorkflowPersistence(
            JdbcTemplate jdbcTemplate,
            DeviceAccountRepository deviceAccountRepository,
            AuditRepository auditRepository,
            UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.deviceAccountRepository = deviceAccountRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RequestListPayload listRequests() {
        List<RequestListItem> list = jdbcTemplate.query(
                """
                select id,
                       request_no,
                       request_type,
                       target_account_name,
                       current_status,
                       request_reason,
                       created_at
                from request_order
                order by created_at desc, id desc
                """,
                (rs, rowNum) -> new RequestListItem(
                        rs.getLong("id"),
                        rs.getString("request_no"),
                        rs.getString("request_type"),
                        rs.getString("target_account_name"),
                        rs.getString("current_status"),
                        rs.getString("request_reason"),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString()));
        return new RequestListPayload(list, list.size());
    }

    @Override
    @Transactional
    public RequestCreateResponse createRequest(
            CreateRequestCommand command,
            Long applicantUserId,
            String applicantUserName,
            String initialState,
            String currentStatusLabel) {
        List<RequestItemCommand> requestItems = normalizedItems(command);
        UserRepository.RequestUserRecord applicant = userRepository.findRequestUserById(applicantUserId)
                .orElseThrow(() -> new IllegalArgumentException("Applicant " + applicantUserId + " does not exist"));
        UserRepository.RequestUserRecord target = userRepository.findRequestUserById(command.targetUserId())
                .orElseThrow(() -> new IllegalArgumentException("Target user " + command.targetUserId() + " does not exist"));
        validateDeviceAccount(target.id(), command.targetDeviceNodeId(), command.targetAccountName());
        DepartmentManagerSnapshot departmentManager = findDepartmentManagerSnapshot(applicant.departmentId());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String tempRequestNo = "TMP-" + UUID.randomUUID();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into request_order (
                      request_no,
                      request_type,
                      applicant_user_id,
                      applicant_department_id,
                      target_user_id,
                      target_department_id,
                      target_device_node_id,
                      target_account_name,
                      request_reason,
                      current_status,
                      current_step,
                      department_manager_snapshot_id,
                      department_manager_snapshot_name,
                      qa_snapshot,
                      qm_snapshot,
                      qi_snapshot,
                      submitted_at,
                      created_at,
                      updated_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null, null, null,
                              current_timestamp, current_timestamp, current_timestamp)
                    """,
                    new String[] {"id"});
            statement.setString(1, tempRequestNo);
            statement.setString(2, command.requestType());
            statement.setLong(3, applicant.id());
            statement.setLong(4, applicant.departmentId());
            statement.setLong(5, target.id());
            statement.setLong(6, target.departmentId());
            statement.setLong(7, command.targetDeviceNodeId());
            statement.setString(8, command.targetAccountName());
            statement.setString(9, command.reason());
            statement.setString(10, initialState);
            statement.setString(11, initialState);
            if (departmentManager.managerUserId() == null) {
                statement.setObject(12, null);
            } else {
                statement.setLong(12, departmentManager.managerUserId());
            }
            statement.setString(13, departmentManager.managerUserName());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create request record");
        }

        long requestId = key.longValue();
        String requestNo = "REQ" + requestId;
        jdbcTemplate.update(
                "update request_order set request_no = ?, updated_at = current_timestamp where id = ?",
                requestNo,
                requestId);
        insertRequestItems(requestId, requestItems);
        auditRepository.insert("REQUEST_CREATED", applicantUserName, "REQUEST", requestId);

        return new RequestCreateResponse(
                requestId,
                requestNo,
                initialState,
                currentStatusLabel,
                requestItems.stream()
                        .map(item -> new RequestWorkflowItem(item.roleNodeId(), ITEM_PENDING))
                        .toList());
    }

    @Override
    public String getRequiredCurrentState(long requestId) {
        String currentState = jdbcTemplate.query(
                "select current_status from request_order where id = ?",
                rs -> rs.next() ? rs.getString("current_status") : null,
                requestId);
        if (currentState == null) {
            throw new IllegalArgumentException("Request " + requestId + " does not exist");
        }
        return currentState;
    }

    @Override
    public boolean compareAndSet(long requestId, String expectedState, String nextState) {
        int updated = jdbcTemplate.update(
                """
                update request_order
                set current_status = ?,
                    current_step = ?,
                    updated_at = current_timestamp
                where id = ? and current_status = ?
                """,
                nextState,
                nextState,
                requestId,
                expectedState);
        if (updated == 0 && !exists(requestId)) {
            throw new IllegalArgumentException("Request " + requestId + " does not exist");
        }
        return updated == 1;
    }

    @Override
    @Transactional
    public boolean submitExecution(long requestId, String expectedState, String nextState, String operatorName) {
        int updated = jdbcTemplate.update(
                """
                update request_order
                set current_status = ?,
                    current_step = ?,
                    finished_at = current_timestamp,
                    updated_at = current_timestamp
                where id = ? and current_status = ?
                """,
                nextState,
                nextState,
                requestId,
                expectedState);
        if (updated == 0 && !exists(requestId)) {
            throw new IllegalArgumentException("Request " + requestId + " does not exist");
        }
        if (updated == 0) {
            return false;
        }

        RequestExecutionTarget target = jdbcTemplate.query(
                """
                select target_user_id, target_device_node_id, target_account_name
                from request_order
                where id = ?
                """,
                rs -> rs.next()
                        ? new RequestExecutionTarget(
                                rs.getLong("target_user_id"),
                                rs.getLong("target_device_node_id"),
                                rs.getString("target_account_name"))
                        : null,
                requestId);
        if (target == null) {
            throw new IllegalArgumentException("Request " + requestId + " does not exist");
        }

        List<Long> roleNodeIds = jdbcTemplate.query(
                """
                select role_node_id
                from request_order_item
                where request_id = ?
                order by id
                """,
                (rs, rowNum) -> rs.getLong("role_node_id"),
                requestId);
        if (roleNodeIds.isEmpty()) {
            throw new IllegalStateException("Request " + requestId + " does not have executable items");
        }

        for (Long roleNodeId : roleNodeIds) {
            deviceAccountRepository.bindRoleToRequestTarget(
                    requestId,
                    target.targetUserId(),
                    target.targetDeviceNodeId(),
                    target.targetAccountName(),
                    roleNodeId);
        }
        jdbcTemplate.update(
                "update request_order_item set item_status = ?, updated_at = current_timestamp where request_id = ?",
                ITEM_EXECUTED,
                requestId);
        auditRepository.insert("EXECUTION_SUBMITTED", operatorName, "REQUEST", requestId);
        return true;
    }

    private List<RequestItemCommand> normalizedItems(CreateRequestCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (command.requestType() == null || command.requestType().isBlank()) {
            throw new IllegalArgumentException("requestType is required");
        }
        if (command.targetUserId() == null) {
            throw new IllegalArgumentException("targetUserId is required");
        }
        if (command.targetDeviceNodeId() == null) {
            throw new IllegalArgumentException("targetDeviceNodeId is required");
        }
        if (command.targetAccountName() == null || command.targetAccountName().isBlank()) {
            throw new IllegalArgumentException("targetAccountName is required");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("items is required");
        }
        if (command.items().stream().anyMatch(item -> item == null || item.roleNodeId() == null)) {
            throw new IllegalArgumentException("items.roleNodeId is required");
        }
        return List.copyOf(command.items());
    }

    private void validateDeviceAccount(long targetUserId, long targetDeviceNodeId, String targetAccountName) {
        DeviceAccountRepository.RequestTargetAccountRecord targetAccount = deviceAccountRepository
                .findRequestTargetAccount(targetDeviceNodeId, targetAccountName)
                .orElse(null);
        if (targetAccount == null) {
            throw new IllegalArgumentException("Target device account does not exist");
        }
        if (targetAccount.userId() != null && !targetAccount.userId().equals(targetUserId)) {
            throw new BusinessException("Target device account is already bound to a different user");
        }
    }

    private DepartmentManagerSnapshot findDepartmentManagerSnapshot(long departmentId) {
        return jdbcTemplate.query(
                """
                select d.manager_user_id, manager.user_name as manager_user_name
                from sys_department d
                left join sys_user manager on manager.id = d.manager_user_id and manager.deleted = 0
                where d.id = ? and d.deleted = 0
                """,
                rs -> rs.next()
                        ? new DepartmentManagerSnapshot(
                                (Long) rs.getObject("manager_user_id"),
                                rs.getString("manager_user_name"))
                        : new DepartmentManagerSnapshot(null, null),
                departmentId);
    }

    private void insertRequestItems(long requestId, List<RequestItemCommand> requestItems) {
        for (RequestItemCommand item : requestItems) {
            jdbcTemplate.update(
                    """
                    insert into request_order_item (
                      request_id,
                      role_node_id,
                      item_status,
                      created_at,
                      updated_at
                    ) values (?, ?, ?, current_timestamp, current_timestamp)
                    """,
                    requestId,
                    item.roleNodeId(),
                    ITEM_PENDING);
        }
    }

    private boolean exists(long requestId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from request_order where id = ?",
                Integer.class,
                requestId);
        return count != null && count > 0;
    }

    private record RequestExecutionTarget(long targetUserId, long targetDeviceNodeId, String targetAccountName) {}

    private record DepartmentManagerSnapshot(Long managerUserId, String managerUserName) {}
}
