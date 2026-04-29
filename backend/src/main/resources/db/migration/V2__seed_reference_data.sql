CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  action_type VARCHAR(50) NOT NULL,
  operator_name VARCHAR(100) NOT NULL,
  object_type VARCHAR(50) NOT NULL,
  object_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_department (id, department_name, manager_user_id, status, deleted)
VALUES
  (1, 'Assembly Dept', 2, 'ENABLED', 0),
  (2, 'Quality Dept', 3, 'ENABLED', 0);

INSERT INTO sys_user (
  id,
  user_code,
  user_name,
  department_id,
  employment_status,
  login_name,
  password_hash,
  account_status,
  deleted
)
VALUES
  (1, 'EMP000', 'System Admin', 1, 'ACTIVE', 'admin', 'admin123', 'ENABLED', 0),
  (2, 'EMP001', 'Zhang San', 1, 'ACTIVE', 'zhangsan', 'zhangsan123', 'ENABLED', 0),
  (3, 'EMP002', 'Li Si', 2, 'ACTIVE', 'lisi', 'lisi123', 'ENABLED', 0),
  (4, 'EMP003', 'Wang Wu', 1, 'ACTIVE', 'wangwu', 'wangwu123', 'ENABLED', 0);

INSERT INTO asset_node (
  id,
  node_name,
  node_type,
  parent_id,
  level_num,
  path,
  sort_no,
  status,
  is_role_node,
  deleted
)
VALUES
  (1, 'Device Catalog', 'CATEGORY', NULL, 1, '/1', 1, 'ENABLED', 0, 0),
  (100, 'Device A', 'DEVICE', 1, 2, '/1/100', 1, 'ENABLED', 0, 0),
  (300, 'Operator', 'ROLE', 100, 3, '/1/100/300', 1, 'ENABLED', 1, 0),
  (301, 'Technician', 'ROLE', 100, 3, '/1/100/301', 2, 'ENABLED', 1, 0),
  (302, 'Inspector', 'ROLE', 100, 3, '/1/100/302', 3, 'ENABLED', 1, 0);

INSERT INTO device_account (
  id,
  user_id,
  device_node_id,
  account_name,
  account_status,
  source_type,
  remark,
  deleted
)
VALUES
  (1, 2, 100, 'device_a_zhangsan', 'ENABLED', 'MANUAL', 'Seed account', 0),
  (2, 3, 100, 'device_a_lisi', 'ENABLED', 'MANUAL', 'Seed account', 0),
  (3, 4, 100, 'device_a_wangwu', 'ENABLED', 'MANUAL', 'Seed account', 0);

INSERT INTO device_account_role (
  id,
  device_account_id,
  role_node_id,
  relation_status,
  effective_at
)
VALUES
  (1, 1, 300, 'ACTIVE', CURRENT_TIMESTAMP),
  (2, 1, 301, 'ACTIVE', CURRENT_TIMESTAMP),
  (3, 2, 300, 'ACTIVE', CURRENT_TIMESTAMP),
  (4, 2, 302, 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO audit_log (id, action_type, operator_name, object_type, object_id, created_at)
VALUES
  (1, 'REQUEST_CREATED', 'Wang Wu', 'REQUEST', 1, TIMESTAMP '2026-04-22 09:30:00');
