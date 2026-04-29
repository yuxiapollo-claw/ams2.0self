alter table device_account_role drop constraint fk_device_account_role_account;
alter table device_account drop constraint fk_device_account_user;
alter table device_account drop constraint fk_device_account_device_node;

create table device_account_new (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  device_node_id BIGINT NOT NULL,
  account_name VARCHAR(100) NOT NULL,
  account_status VARCHAR(20) NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_device_account_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_device_account_device_node FOREIGN KEY (device_node_id) REFERENCES asset_node (id)
);

insert into device_account_new (
  id,
  user_id,
  device_node_id,
  account_name,
  account_status,
  source_type,
  remark,
  created_at,
  updated_at,
  deleted
)
select id,
       user_id,
       device_node_id,
       account_name,
       account_status,
       source_type,
       remark,
       created_at,
       updated_at,
       deleted
from device_account;

drop table device_account;
alter table device_account_new rename to device_account;
create index idx_device_account_user on device_account (user_id);
create index idx_device_account_device_node on device_account (device_node_id);

alter table device_account_role
  add constraint fk_device_account_role_account
  foreign key (device_account_id) references device_account (id);
