CREATE TABLE request_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  role_node_id BIGINT NOT NULL,
  item_status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_request_order_item_request (request_id),
  KEY idx_request_order_item_role (role_node_id),
  CONSTRAINT fk_request_order_item_request FOREIGN KEY (request_id) REFERENCES request_order (id),
  CONSTRAINT fk_request_order_item_role FOREIGN KEY (role_node_id) REFERENCES asset_node (id)
);

UPDATE sys_user
SET password_hash = '$2a$10$mMQ4LA/fmGkbcXoS0ZITM.QOjGsgIo9ECYRHi2ucTBOdmI/BvCyPq'
WHERE login_name = 'admin';

UPDATE sys_user
SET password_hash = '$2a$10$heYFmopHDIcxp1j/H3ahGuTdkKyVrmFwx.rrhSm6ls7KIfn6bTbki'
WHERE login_name = 'zhangsan';

UPDATE sys_user
SET password_hash = '$2a$10$akAvmRJu1TzIfyDsZgLSf.gQl0m3omjo6OGJzMV/x5u0TLEBioLo.'
WHERE login_name = 'lisi';

UPDATE sys_user
SET password_hash = '$2a$10$ZSevsEroGnjDi0ZoHjVhlulcnZEROB.SacNu1mY47040z3vAOWaSO'
WHERE login_name = 'wangwu';
