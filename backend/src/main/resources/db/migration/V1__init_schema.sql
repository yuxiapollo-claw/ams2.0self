CREATE TABLE sys_department (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  department_name VARCHAR(100) NOT NULL,
  manager_user_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_code VARCHAR(64) NOT NULL,
  user_name VARCHAR(100) NOT NULL,
  department_id BIGINT NOT NULL,
  employment_status VARCHAR(20) NOT NULL,
  login_name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  account_status VARCHAR(20) NOT NULL,
  last_login_time TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_code (user_code),
  UNIQUE KEY uk_sys_user_login (login_name),
  KEY idx_sys_user_department (department_id),
  CONSTRAINT fk_sys_user_department FOREIGN KEY (department_id) REFERENCES sys_department (id)
);

CREATE TABLE asset_node (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  node_name VARCHAR(100) NOT NULL,
  node_type VARCHAR(30) NOT NULL,
  parent_id BIGINT NULL,
  level_num INT NOT NULL,
  path VARCHAR(500) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  is_role_node TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_asset_node_parent (parent_id),
  CONSTRAINT fk_asset_node_parent FOREIGN KEY (parent_id) REFERENCES asset_node (id)
);

CREATE TABLE device_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_node_id BIGINT NOT NULL,
  account_name VARCHAR(100) NOT NULL,
  account_status VARCHAR(20) NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_device_account_user_device (user_id, device_node_id),
  KEY idx_device_account_device_node (device_node_id),
  CONSTRAINT fk_device_account_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_device_account_device_node FOREIGN KEY (device_node_id) REFERENCES asset_node (id)
);

CREATE TABLE request_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_no VARCHAR(64) NOT NULL,
  request_type VARCHAR(30) NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  applicant_department_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  target_department_id BIGINT NOT NULL,
  target_device_node_id BIGINT NOT NULL,
  target_account_name VARCHAR(100) NULL,
  request_reason VARCHAR(255) NOT NULL,
  current_status VARCHAR(30) NOT NULL,
  current_step VARCHAR(30) NOT NULL,
  department_manager_snapshot_id BIGINT NULL,
  department_manager_snapshot_name VARCHAR(100) NULL,
  qa_snapshot VARCHAR(255) NULL,
  qm_snapshot VARCHAR(255) NULL,
  qi_snapshot VARCHAR(255) NULL,
  submitted_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_request_order_no (request_no),
  KEY idx_request_order_applicant_user (applicant_user_id),
  KEY idx_request_order_applicant_department (applicant_department_id),
  KEY idx_request_order_target_user (target_user_id),
  KEY idx_request_order_target_department (target_department_id),
  KEY idx_request_order_target_device_node (target_device_node_id),
  KEY idx_request_order_current_status (current_status),
  CONSTRAINT fk_request_order_applicant_user FOREIGN KEY (applicant_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_request_order_applicant_department FOREIGN KEY (applicant_department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_request_order_target_user FOREIGN KEY (target_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_request_order_target_department FOREIGN KEY (target_department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_request_order_target_device_node FOREIGN KEY (target_device_node_id) REFERENCES asset_node (id)
);

CREATE TABLE device_account_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_account_id BIGINT NOT NULL,
  role_node_id BIGINT NOT NULL,
  relation_status VARCHAR(20) NOT NULL,
  effective_at TIMESTAMP NULL,
  expired_at TIMESTAMP NULL,
  source_request_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_device_account_role (device_account_id, role_node_id),
  KEY idx_device_account_role_role_node (role_node_id),
  KEY idx_device_account_role_source_request (source_request_id),
  CONSTRAINT fk_device_account_role_account FOREIGN KEY (device_account_id) REFERENCES device_account (id),
  CONSTRAINT fk_device_account_role_role_node FOREIGN KEY (role_node_id) REFERENCES asset_node (id),
  CONSTRAINT fk_device_account_role_source_request FOREIGN KEY (source_request_id) REFERENCES request_order (id)
);
