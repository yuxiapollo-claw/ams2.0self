# AMS2.0 UI CRUD 冒烟清单

## 自动化校验

在声明候选版本可发布前，于工作区根目录执行以下命令：

```powershell
mvn -f backend/pom.xml test
npm --prefix frontend test
npm --prefix frontend run build
npm --prefix frontend run test:e2e -- login.spec.ts cockpit-navigation.spec.ts
docker compose up --build -d
docker compose ps
```

期望结果：

1. 后端测试以退出码 `0` 完成。
2. 前端单元测试以退出码 `0` 完成。
3. 前端生产构建以退出码 `0` 完成。
4. Playwright UI 冒烟通过，`login.spec.ts` 与 `cockpit-navigation.spec.ts` 均为绿色。
5. `docker compose up --build -d` 执行完成，容器无崩溃重启。
6. `docker compose ps` 显示 `backend` 与 `frontend` 均处于 `Up` 状态。

## 手工冒烟

1. 打开 `http://127.0.0.1:5173/login`，确认登录页展示中文品牌区、登录名输入框、密码输入框与“进入系统”按钮。
2. 使用 `zhangsan / zhangsan123` 登录，确认驾驶舱壳层成功加载，侧边栏、顶部禁用全局搜索框以及语言/主题切换按钮可见。
3. 在任意登录后的页面点击语言切换按钮，确认界面可在 `中文` 与 `English` 间切换，页面标题和导航文案同步更新。
4. 点击主题切换按钮，确认界面可在暗黑与明亮主题间切换，且切换后布局、表单、表格可正常阅读。
5. 打开 `/users`，创建一个用户，编辑同一用户，然后停用或重新启用账号，确认列表刷新后状态正确。
6. 打开 `/departments`，创建一个部门，更新部门说明，并确认当成员阻止删除时，受保护删除仍返回明确错误。
7. 打开 `/device-accounts`，创建一个未绑定账号，再编辑为绑定用户，确认列表刷新后绑定关系和角色状态保持最新。
8. 在一个已绑定的设备账号行点击“发起加权申请”，确认新建申请表单已预填目标用户、设备与账号。
9. 提交一条申请，然后确认 `/requests` 出现新记录，`/audit/logs` 出现对应审计记录。
10. 在执行 `docker compose up --build -d` 后，重复核心浏览器路径：登录、打开 `/users`、打开 `/device-accounts`、发起加权申请，确认无运行时错误。

## 说明

1. Playwright 冒烟使用独立 Vite 端口 `4173`，避免 Docker 暴露的 `5173` 掩盖当前前端源码。
2. Playwright 套件属于 UI 冒烟层，主要覆盖登录页、驾驶舱导航、语言切换与主题切换；真实认证与后端联调仍通过 Docker 运行态下的手工冒烟确认。
3. 手工冒烟继续使用 Docker 运行态端口 `5173`，因为这是更接近发布环境的访问路径。
