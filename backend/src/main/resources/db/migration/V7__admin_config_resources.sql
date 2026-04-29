CREATE TABLE admin_application_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_name VARCHAR(100) NOT NULL,
  application_code VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_admin_application_config_code (application_code)
);

CREATE TABLE admin_mail_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  subject VARCHAR(255) NOT NULL,
  body VARCHAR(4000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_admin_mail_template_name (template_name)
);

INSERT INTO admin_application_config (
  id,
  application_name,
  application_code,
  description,
  status,
  deleted
)
VALUES
  (1, 'DMS', 'APP-DMS', 'Document management system', 'ENABLED', 0),
  (2, 'QMS', 'APP-QMS', 'Quality management system', 'ENABLED', 0);

INSERT INTO admin_mail_template (
  id,
  template_name,
  description,
  subject,
  body,
  status,
  deleted
)
VALUES
  (1, '审批待办通知', '待办审批提醒', '【AMS】审批待办', '您有新的审批待办，请及时处理。', 'ENABLED', 0),
  (2, '账号变更通知', '账号状态变更提醒', '【AMS】账号变更通知', '您的系统账号状态已发生变更。', 'ENABLED', 0);
