alter table sys_user
  add column is_system_admin TINYINT NOT NULL DEFAULT 0;

update sys_user
set is_system_admin = 1
where login_name = 'admin';

create table access_system (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  system_name VARCHAR(100) NOT NULL,
  system_description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_access_system_name (system_name)
);

create table access_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  system_id BIGINT NOT NULL,
  parent_permission_id BIGINT NULL,
  permission_name VARCHAR(100) NOT NULL,
  full_path VARCHAR(500) NOT NULL,
  level_num INT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_access_permission_system (system_id),
  KEY idx_access_permission_parent (parent_permission_id),
  CONSTRAINT fk_access_permission_system FOREIGN KEY (system_id) REFERENCES access_system (id),
  CONSTRAINT fk_access_permission_parent FOREIGN KEY (parent_permission_id) REFERENCES access_permission (id)
);

create table user_permission_assignment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_permission_assignment (user_id, permission_id),
  KEY idx_user_permission_assignment_permission (permission_id),
  CONSTRAINT fk_user_permission_assignment_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_user_permission_assignment_permission FOREIGN KEY (permission_id) REFERENCES access_permission (id)
);

create table access_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_no VARCHAR(64) NOT NULL,
  request_type VARCHAR(40) NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  applicant_department_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  permission_path_snapshot VARCHAR(500) NOT NULL,
  request_reason VARCHAR(255) NOT NULL,
  current_status VARCHAR(40) NOT NULL,
  current_step VARCHAR(40) NOT NULL,
  department_leader_snapshot_id BIGINT NULL,
  department_leader_snapshot_name VARCHAR(100) NULL,
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_access_request_no (request_no),
  KEY idx_access_request_applicant_user (applicant_user_id),
  KEY idx_access_request_target_user (target_user_id),
  KEY idx_access_request_permission (permission_id),
  CONSTRAINT fk_access_request_applicant_user FOREIGN KEY (applicant_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_access_request_applicant_department FOREIGN KEY (applicant_department_id) REFERENCES sys_department (id),
  CONSTRAINT fk_access_request_target_user FOREIGN KEY (target_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_access_request_permission FOREIGN KEY (permission_id) REFERENCES access_permission (id)
);

insert into access_system (id, system_name, system_description, deleted)
values
  (1, 'MES', '制造执行系统', 0),
  (2, 'LIMS', '实验室信息管理系统', 0);

insert into access_permission (
  id,
  system_id,
  parent_permission_id,
  permission_name,
  full_path,
  level_num,
  enabled,
  deleted
)
values
  (1, 1, null, '生产管理', 'MES/生产管理', 2, 1, 0),
  (2, 1, 1, '工单维护', 'MES/生产管理/工单维护', 3, 1, 0),
  (3, 1, 1, '设备报工', 'MES/生产管理/设备报工', 3, 1, 0),
  (4, 2, null, '质量主数据', 'LIMS/质量主数据', 2, 1, 0),
  (5, 2, 4, '样本创建', 'LIMS/质量主数据/样本创建', 3, 1, 0),
  (6, 2, 4, '结果审批', 'LIMS/质量主数据/结果审批', 3, 1, 0);

insert into user_permission_assignment (user_id, permission_id)
values
  (1, 2),
  (1, 5),
  (2, 2),
  (2, 3),
  (3, 5),
  (4, 6);
