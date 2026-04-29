# AMS 目标系统 1:1 复刻设计文档

> 目标：以 `http://172.18.200.153` 上现有业务系统为只读参照，在当前仓库内复刻一个结构、路由、文案、交互和业务域等价的权限管理平台，并作为后续实施计划的唯一设计基线。

## 1. 文档信息

- 文档日期：2026-04-29
- 调研方式：Playwright 只读登录与路由巡检
- 目标系统地址：`http://172.18.200.153`
- 调研账号：`AMSAdmin`
- 调研限制：只查看，不新增、不修改、不删除、不提交任何业务数据
- 设计结论：以 `AMSAdmin` 当前可见页面范围作为第一轮全量复刻基线

## 2. 背景与结论

当前仓库中的 AMS 项目是一个简化版权限申请系统，覆盖了登录、控制台、用户、部门、设备账号、申请、查询和审计等有限页面，后端也主要实现了简化的申请流与基础查询能力。目标系统则是一个完整的 IAM 运营平台，已确认包含控制台、权限管理、任务管理、报告、系统/权限经理、系统管理员、人事信息管理员等完整业务域。

这意味着本次工作不能沿用“在现有页面上修补”的思路，也不能继续沿用之前偏现代化、重新设计式的 UI 方向。正确方向是保留当前技术栈与工程骨架，但将产品信息架构、路由树、页面模板、后端领域模型和数据库模型全部重建到与目标系统等价的层级。

## 3. 调研范围与只读原则

本次调研通过 Playwright 登录目标系统后，仅进行了以下类型操作：

- 登录
- 一级导航切换
- 通过 hash 路由直接进入页面
- 读取页面标题、按钮、标签、表头、占位符和页面正文摘要

明确避免了以下风险操作：

- `申请`
- `删除`
- `重置密码`
- `修改`
- `同意`
- `驳回`
- `转发`
- `完成`
- `申诉`
- `导入权限列表`
- 任何可能触发提交、保存、导出写库、审批或状态流转的行为

因此，本文档基于只读调研结果，适合作为复刻设计依据，但不应被误解为已验证目标系统每个表单的写入规则和异常提示细节。写入态细节需要在后续复刻实施阶段，通过本地实现与受控联调逐步补齐。

## 4. 当前项目与目标系统的差距

### 4.1 当前项目现状

前端当前采用：

- Vue 3
- Pinia
- Vue Router
- Axios
- Element Plus

后端当前采用：

- Java 17
- Spring Boot 3
- Spring Security
- JDBC
- Flyway
- H2 / MySQL

现有系统更接近“设备账号申请与查询系统”，核心边界仍停留在：

- 用户与部门基础管理
- 设备账号列表
- 简化申请单
- 简化审计日志
- 少量查询页

### 4.2 目标系统特征

目标系统不是单一申请流产品，而是企业内权限治理平台，具备以下特征：

- 多角色工作台
- 分域导航结构
- 申请、审批、执行、审核、申诉等多条业务主线
- 工作流与工作组配置
- 系统与权限配置管理
- 报表查询与导出
- 代理授权
- HR 主数据与组织维护
- 中英文切换
- 高密度表格化企业后台风格

### 4.3 核心设计判断

本次复刻必须按“整个平台重建”处理，而不是按“美化现有页面”处理。应保留的是工程技术栈与部署方式，应推翻的是当前过于简化的信息架构和页面体系。

## 5. 目标系统信息架构

经只读调研，当前账号 `AMSAdmin` 可见的第一轮复刻基线共有 31 个主要页面，分布如下。

### 5.1 一级导航

- 控制台
- 权限管理
- 任务管理
- 报告
- 系统/权限经理
- 系统管理员
- 人事信息管理员

### 5.2 二级页面清单

#### 控制台

- `#/dashboard`

#### 权限管理

- `#/access/myRequest`
- `#/access/myRemove`
- `#/access/myChangepd`
- `#/access/myView`
- `#/access/accountManagement`
- `#/access/reviewCheck`
- `#/access/bedelegation`

#### 任务管理

- `#/task/accessApproval`
- `#/task/accessOperation`
- `#/task/reviewApproval`
- `#/task/revocation`

#### 报告

- `#/report/accountHistory`
- `#/report/reviewReport`
- `#/report/empAccess`
- `#/report/systemAccess`
- `#/report/sysconfig`
- `#/report/variationReport`

#### 系统/权限经理

- `#/sysAccessManager/empAccessManagement`
- `#/sysAccessManager/reviewCheck`

#### 系统管理员

- `#/systemAdmin/userAccessManagement`
- `#/systemAdmin/empAccessManagement`
- `#/systemAdmin/copyProfile`
- `#/systemAdmin/sysAccessAdmin`
- `#/systemAdmin/reviewManagement`
- `#/systemAdmin/admindelegation`
- `#/systemAdmin/adminworkflow`
- `#/systemAdmin/applicationConfig`
- `#/systemAdmin/mailTemplateCfg`

#### 人事信息管理员

- `#/hrAdmin/hrManagement`
- `#/hrAdmin/enterprisemaintain`

## 6. 全局壳层设计

目标系统的整体壳层不是侧边栏主导型，而是“顶部一级导航 + 左侧上下文菜单 + 中部内容区”的企业后台结构。复刻时应按该结构重建，而不是继续沿用当前更偏应用首页式的壳层。

### 6.1 登录页

登录页应复刻为目标系统的简洁布局：

- 页面标题：`LocalIAMSolution`
- 页面主文案：`权限管理系统`
- 输入项：`用户名`、`密码`
- 主按钮：`登录`

不再保留现有项目中额外的品牌化视觉扩展、营销式副文案或与目标系统不一致的布局元素。

### 6.2 顶部区域

顶部区域应包含：

- 一级导航
- 当前用户：`AMSAdmin`
- 语言切换：`中文` / `English`
- 版本号：`版本: 1.0.0`

### 6.3 左侧区域

左侧区域是当前一级菜单下的上下文子菜单。切换一级导航后，左侧子菜单应整体切换，而不是只做高亮变化。

### 6.4 内容区

内容区统一采用：

- 面包屑或路径提示
- 页面标题
- 页面说明文案
- 高密度表格 / 树 / 标签页 / 筛选栏

### 6.5 国际化

应保留双语切换能力，但目标基线以“默认中文、可切英文”为准。当前仓库已有中英切换基础，后续需重构文案资源，保证与目标系统文案一致，而不是仅做直译。

## 7. 页面模板设计

为避免 31 个页面全部独立开发，建议按目标系统抽象出 8 类页面模板。

### 7.1 模板 A：控制台总览页

适用页面：

- `#/dashboard`

特征：

- 欢迎区
- 系统公告
- 待办事项聚合
- 待审批、待操作、待审核、待申诉计数卡

### 7.2 模板 B：账号上下文权限操作页

适用页面：

- `#/access/myRequest`
- `#/access/myRemove`
- `#/access/myChangepd`
- `#/access/myView`

特征：

- 当前操作账号提示
- 权限树或权限列表
- 搜索框
- 标签页
- 操作按钮

### 7.3 模板 C：账号列表与行内操作页

适用页面：

- `#/access/accountManagement`
- `#/systemAdmin/userAccessManagement`

特征：

- 表格为主
- 每行提供多个业务动作
- 列表支持筛选
- 后续可进入申请、删除、重置、查看、修改所属人等流程

### 7.4 模板 D：任务队列页

适用页面：

- `#/task/accessApproval`
- `#/task/accessOperation`
- `#/task/reviewApproval`
- `#/task/revocation`

特征：

- 表格任务列表
- 顶部批量或单行操作
- 部分页面存在标签页
- 每个页面使用不同操作集

### 7.5 模板 E：报表查询与导出页

适用页面：

- `#/report/accountHistory`
- `#/report/reviewReport`
- `#/report/empAccess`
- `#/report/systemAccess`
- `#/report/sysconfig`
- `#/report/variationReport`

特征：

- 查询条件区
- 结果表格
- 导出按钮
- 部分页面有生成报告、导入对比列表等流程入口

### 7.6 模板 F：管理者视角权限运营页

适用页面：

- `#/sysAccessManager/empAccessManagement`
- `#/sysAccessManager/reviewCheck`

特征：

- 仅展示本人管理范围
- 强调“我管理的系统/权限”
- 支持申请与审核查看

### 7.7 模板 G：配置管理 CRUD 页

适用页面：

- `#/systemAdmin/empAccessManagement`
- `#/systemAdmin/copyProfile`
- `#/systemAdmin/sysAccessAdmin`
- `#/systemAdmin/reviewManagement`
- `#/systemAdmin/admindelegation`
- `#/systemAdmin/adminworkflow`
- `#/systemAdmin/applicationConfig`
- `#/systemAdmin/mailTemplateCfg`

特征：

- 筛选 + 表格 + 弹窗详情/编辑
- 标签页切换
- 新增、修改、删除、详情、复制等管理动作

### 7.8 模板 H：HR 主数据页

适用页面：

- `#/hrAdmin/hrManagement`
- `#/hrAdmin/enterprisemaintain`

特征：

- 大数据量表格
- 组织架构或部门主数据
- 员工、部门负责人、状态、成员查看
- 修改信息、修改部门、详情等动作

## 8. 业务域设计

### 8.1 控制台域

职责：

- 展示当前登录人的工作台
- 聚合待办计数
- 展示公告和快捷跳转入口

核心要求：

- 仪表盘数据必须来自真实业务统计，而不是前端静态拼接

### 8.2 权限管理域

职责：

- 普通用户自助发起权限相关业务
- 查看本人权限、本人账号、本人被授权情况

核心子能力：

- 申请权限
- 删除权限
- 重置密码
- 查看权限
- 账号列表
- 审核查看
- 被授权查看

### 8.3 任务管理域

职责：

- 面向审批人、执行人、审核相关角色处理待办

核心子能力：

- 审批任务
- 操作任务
- 审核任务
- 申诉任务

### 8.4 报告域

职责：

- 提供面向运营和审计的查询与导出能力

核心子能力：

- 账号历史
- 审核报告
- 用户权限报告
- 系统用户报告
- 系统配置报告
- 权限差异报告

### 8.5 系统/权限经理域

职责：

- 面向系统或权限负责人查看自己负责的权限范围
- 发起与自己职责相关的运营动作

### 8.6 系统管理员域

职责：

- 全局视角下管理用户权限、系统权限、工作流、代理、应用配置、邮件模板和审核计划

### 8.7 人事信息管理员域

职责：

- 管理员工主数据
- 管理企业组织结构

## 9. 后端领域重构设计

当前后端仍以简化申请流为核心，需升级为平台化领域结构。建议保留 Spring Boot 单体架构，但按业务域重新分包和建模。

建议后端领域模块为：

- `auth`：登录、会话、当前用户、语言偏好
- `dashboard`：控制台统计与公告
- `access`：自助权限申请、删除、重置密码、查看
- `account`：账号及账号权限关系
- `task`：审批、执行、审核、申诉任务
- `report`：查询、导出、报表任务
- `manager`：系统/权限经理工作台
- `admin`：系统管理员配置域
- `hr`：员工与组织主数据
- `workflow`：工作流定义、工作组、步骤、任务生成
- `delegation`：代理授权
- `review`：审核计划、审核结果、审核任务
- `mail`：邮件模板与通知编排
- `audit`：操作日志与业务审计
- `common`：统一返回体、分页、异常、字典、枚举

## 10. 数据模型重构设计

当前 ER 仅覆盖简化模型，无法支撑目标系统的多域配置能力。建议从“基础主数据 + 业务关系 + 流程引擎 + 审核与报表 + 系统配置”五个层面扩展。

### 10.1 基础主数据

- `iam_user`
- `iam_department`
- `iam_department_member`
- `iam_department_history`
- `iam_application`
- `iam_system`
- `iam_permission_node`
- `iam_account`

### 10.2 账号与权限关系

- `iam_account_permission`
- `iam_account_owner_history`
- `iam_account_reset_record`

### 10.3 申请与任务

- `iam_request_order`
- `iam_request_order_item`
- `iam_task`
- `iam_task_action_log`
- `iam_appeal_order`

### 10.4 工作流与工作组

- `iam_workflow`
- `iam_workflow_step`
- `iam_workgroup`
- `iam_workgroup_member`
- `iam_business_workflow_binding`

### 10.5 审核与代理

- `iam_review_plan`
- `iam_review_item`
- `iam_review_action`
- `iam_delegation`

### 10.6 配置与通知

- `iam_mail_template`
- `iam_system_config`
- `iam_export_job`
- `iam_i18n_resource`

### 10.7 审计

- `iam_audit_log`
- `iam_login_log`

## 11. 核心业务流设计

### 11.1 自助权限申请流

流程：

1. 用户选择账号或进入账号上下文
2. 选择目标权限路径
3. 填写申请原因
4. 创建申请单与明细
5. 根据绑定工作流生成审批任务
6. 审批通过后进入执行任务
7. 执行完成后生效

### 11.2 权限删除流

流程与申请流类似，但输入对象改为当前已获得权限，生效动作改为回收权限关系。

### 11.3 密码重置流

流程与申请流类似，但目标对象为账号，执行阶段写入密码重置记录。系统应记录重置动作与结果，但不保存明文密码。

### 11.4 审批流

从目标系统已暴露动作可知，审批流至少需要支持：

- 同意
- 驳回
- 退回
- 转发

因此任务设计不能只做 `approve/reject` 两态，而应支持任务重定向、转签和退回链路。

### 11.5 执行流

执行任务至少支持：

- 完成
- 拒绝
- 导出报告

执行结果应写回申请单和任务明细，并在失败时进入可追踪状态，而不是简单抛错结束。

### 11.6 审核流

应单独建模：

- 审核计划
- 审核范围
- 审核任务
- 审核结果
- 申诉与复审

不能把“审核”混在普通申请审批表里处理。

### 11.7 代理流

系统需支持：

- 被授权查看
- 管理员代理配置
- 代理有效期
- 代理状态

代理关系应影响任务分发与可见范围。

## 12. 前端重构设计

### 12.1 路由策略

建议切换到与目标系统一致的 hash 路由风格，以便后续：

- 页面对照
- Playwright 自动巡检
- 与目标系统截图和 DOM 文本比对

建议新路由按目标路径重建，不再沿用当前 `/users`、`/departments`、`/requests` 这种简化式路径。

### 12.2 页面组织

建议前端目录按业务域拆分：

- `views/dashboard`
- `views/access`
- `views/task`
- `views/report`
- `views/sys-access-manager`
- `views/system-admin`
- `views/hr-admin`
- `views/auth`

组件层按模板沉淀：

- `components/layout`
- `components/page-header`
- `components/data-table`
- `components/filter-bar`
- `components/detail-drawer`
- `components/permission-tree`
- `components/task-actions`

### 12.3 状态管理

Pinia store 应按域拆分：

- `authStore`
- `layoutStore`
- `dictionaryStore`
- `dashboardStore`
- `accessStore`
- `taskStore`
- `reportStore`
- `adminStore`
- `hrStore`

### 12.4 UI 风格要求

UI 不应再继续偏“重新设计”，而应以目标系统为准：

- 色彩保守
- 按钮密度高
- 表格优先
- 信息分区清晰
- 弱化营销式视觉
- 强化业务操作效率

## 13. 接口设计原则

### 13.1 统一返回结构

延续现有接口包络：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "local-dev"
}
```

### 13.2 接口分组

- `/api/auth`
- `/api/dashboard`
- `/api/access`
- `/api/accounts`
- `/api/tasks`
- `/api/reports`
- `/api/manager`
- `/api/admin`
- `/api/hr`
- `/api/workflows`
- `/api/reviews`
- `/api/delegations`
- `/api/mail-templates`
- `/api/audit`

### 13.3 设计原则

- 读写分离
- 列表统一分页
- 表单详情尽量聚合返回
- 所有状态变更都要求记录操作日志
- 导出走异步任务，避免阻塞大查询

## 14. 报表与导出设计

目标系统已经明确暴露出 `PDF`、`Excel`、`导出报告`、`生成报告`、`导入权限列表` 等能力，因此复刻时应按“真实导出能力”处理，而不是前端导出当前页表格。

建议导出设计：

- 小数据量：同步导出
- 大数据量：异步生成导出任务
- 文件格式：至少支持 Excel，审核报告支持 PDF
- 导出留痕：必须写审计日志

## 15. 安全与权限设计

### 15.1 功能权限

控制：

- 一级导航可见性
- 二级菜单可见性
- 页面按钮可见性
- 接口调用权限

### 15.2 数据权限

控制：

- 普通用户仅看本人数据
- 管理者看其管理范围
- 系统管理员看全局
- HR 管理员看 HR 数据范围

### 15.3 审计要求

以下动作必须记录：

- 登录成功/失败
- 申请创建
- 审批动作
- 执行动作
- 审核动作
- 代理配置
- 系统配置变更
- 导出操作

## 16. 测试设计

### 16.1 Playwright 对照测试

将目标系统研究方法转化为本地验收方法，至少覆盖：

- 登录页元素比对
- 一级导航比对
- 二级菜单比对
- 页面标题比对
- 主按钮文案比对
- 表头抽样比对
- 标签页抽样比对
- 语言切换比对

### 16.2 后端测试

- 领域服务单测
- 工作流状态流转测试
- 权限校验测试
- 关键接口集成测试

### 16.3 前端测试

- 路由守卫
- 菜单切换
- 表单提交
- 列表筛选
- 弹窗与详情联动

## 17. 分阶段实施建议

### Phase 0：基线冻结

- 冻结目标系统页面地图
- 冻结路由清单
- 冻结一级导航与二级菜单
- 冻结首批页面字段矩阵

### Phase 1：认证与主框架

- 登录页重建
- 顶部导航重建
- 左侧上下文菜单重建
- 中英文切换接入

### Phase 2：权限管理域

- 申请权限
- 删除权限
- 重置密码
- 查看权限
- 账号列表
- 审核查看
- 被授权查看

### Phase 3：任务管理域

- 审批任务
- 操作任务
- 审核任务
- 申诉任务

### Phase 4：报告域

- 六类报表查询与导出

### Phase 5：经理与管理员域

- 系统/权限经理两页
- 系统管理员九页
- 工作流与应用配置
- 邮件模板配置

### Phase 6：HR 域

- 人事信息管理
- 企业架构管理

### Phase 7：收口与上线

- Playwright 回归
- 性能与分页优化
- MySQL 持久化验证
- Docker 部署验证

## 18. 风险与约束

### 18.1 已知风险

- 当前只读调研缺少大量非空态细节
- 目标系统部分写入弹窗和表单细节尚未验证
- 当前本地后端模型与目标系统差距较大，重构量明显高于单纯前端改版

### 18.2 设计约束

- 第一轮复刻以 `AMSAdmin` 可见范围为基线
- 不追求超越目标系统的“重新设计”
- 不引入微服务拆分
- 不在第一轮强制接入 LDAP/AD/SSO

## 19. 验收标准

复刻完成后，应至少满足以下标准：

- 当前账号可见的 31 个页面全部可访问
- 一级导航与二级菜单结构与目标系统一致
- 登录页、控制台、权限管理、任务管理、报告、管理员、HR 七大域齐备
- 主要按钮名称、主要表头和关键标签页与目标系统一致
- 中英文切换位置和行为一致
- 核心流程具备真实后端支撑，不是静态页面
- Playwright 对照测试通过

## 20. 设计结论

本项目后续实施应以“保留技术栈，重建产品基线”为核心原则。现有仓库可以继续作为承载体，但旧的简化页面体系和偏重新设计式 UI 不再作为依据。唯一正确的实施路径，是以本文档定义的 31 页复刻范围、7 大业务域、8 类页面模板、平台化后端模型和分阶段实施计划作为新的统一基线。
