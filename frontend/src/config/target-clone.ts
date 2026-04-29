export interface TargetCloneEntry {
  path: string
  sectionKey: string
  sectionLabel: string
  title: string
  description: string
  buttons?: string[]
  tabs?: string[]
  headers?: string[]
}

export interface TargetCloneSection {
  key: string
  label: string
  entries: TargetCloneEntry[]
}

export const targetCloneSections: TargetCloneSection[] = [
  {
    key: 'dashboard',
    label: '控制台',
    entries: [
      {
        path: '/dashboard',
        sectionKey: 'dashboard',
        sectionLabel: '控制台',
        title: '控制台',
        description: '系统工作台'
      }
    ]
  },
  {
    key: 'access',
    label: '权限管理',
    entries: [
      {
        path: '/access/myRequest',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '申请权限',
        description: '当前正在为账号 AMSAdmin 申请权限',
        buttons: ['申请'],
        headers: ['权限路径', '全部展开']
      },
      {
        path: '/access/myRemove',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '删除权限',
        description: '当前正在为账号 AMSAdmin 删除权限',
        buttons: ['删除'],
        headers: ['权限路径']
      },
      {
        path: '/access/myChangepd',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '重置密码',
        description: '当前正在为账号 AMSAdmin 重置密码',
        buttons: ['重置密码'],
        headers: ['权限路径']
      },
      {
        path: '/access/myView',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '查看权限',
        description: '当前正在查看账号 AMSAdmin 的权限',
        tabs: ['已获得', '申请中'],
        headers: ['权限路径', '设备账号', '详情', '申请类型', '撤销']
      },
      {
        path: '/access/accountManagement',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '账号列表',
        description: 'AMSAdmin 拥有的账号如下',
        buttons: ['申请权限', '重置密码', '删除权限', '查看权限', '修改所属人'],
        headers: ['前缀', '账号名称', '账号类型', '账号描述', '申请权限', '重置密码', '删除权限', '查看权限', '修改所属人']
      },
      {
        path: '/access/reviewCheck',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '审核查看',
        description: '当前正在查看 AMSAdmin 负责的审核',
        headers: ['系统名称', '审核名称', '审核类型', '开始日期', '结束日期', '详情']
      },
      {
        path: '/access/bedelegation',
        sectionKey: 'access',
        sectionLabel: '权限管理',
        title: '被授权查看',
        description: '被授权',
        headers: ['用户名', '用户全称', '开始日期', '结束日期', '状态']
      }
    ]
  },
  {
    key: 'task',
    label: '任务管理',
    entries: [
      {
        path: '/task/accessApproval',
        sectionKey: 'task',
        sectionLabel: '任务管理',
        title: '审批任务',
        description: 'AMSAdmin 需要审批的权限如下',
        buttons: ['退回', '转发', '驳回', '同意'],
        headers: ['申请类型', '权限路径', '账号名称', '设备账号', '账号所属人', '开始时间', '详情']
      },
      {
        path: '/task/accessOperation',
        sectionKey: 'task',
        sectionLabel: '任务管理',
        title: '操作任务',
        description: 'AMSAdmin 需要处理的权限如下',
        buttons: ['导出报告', '拒绝', '完成'],
        tabs: ['待操作', '已完成'],
        headers: ['操作类型', '权限路径', '账号名称', '设备账号', '账号所属人', '自动化失败', '开始时间', '详情']
      },
      {
        path: '/task/reviewApproval',
        sectionKey: 'task',
        sectionLabel: '任务管理',
        title: '审核任务',
        description: '审核审批任务',
        headers: ['系统名称', '审核名称', '审核类型', '审批开始日期', '审批结束日期', '审批']
      },
      {
        path: '/task/revocation',
        sectionKey: 'task',
        sectionLabel: '任务管理',
        title: '申诉任务',
        description: '申诉任务处理',
        buttons: ['放弃', '申诉'],
        tabs: ['待提交', '待复审'],
        headers: ['审核名称', '权限路径', '账号名称', '权限驳回人', '申诉结束日期', '账号所属人', '详情']
      }
    ]
  },
  {
    key: 'report',
    label: '报告',
    entries: [
      {
        path: '/report/accountHistory',
        sectionKey: 'report',
        sectionLabel: '报告',
        title: '账号历史',
        description: '账号权限历史',
        headers: ['用户', '部门']
      },
      {
        path: '/report/reviewReport',
        sectionKey: 'report',
        sectionLabel: '报告',
        title: '审核报告',
        description: '审核导出报告',
        buttons: ['PDF', 'Excel'],
        headers: ['系统名称', '审核开始区间']
      },
      {
        path: '/report/empAccess',
        sectionKey: 'report',
        sectionLabel: '报告',
        title: '用户权限报告',
        description: '用户权限报告查询',
        buttons: ['搜索', '导出'],
        headers: ['用户', '部门']
      },
      {
        path: '/report/systemAccess',
        sectionKey: 'report',
        sectionLabel: '报告',
        title: '系统用户报告',
        description: '系统用户报告查询',
        buttons: ['搜索', '导出报告'],
        headers: ['系统名称']
      },
      {
        path: '/report/sysconfig',
        sectionKey: 'report',
        sectionLabel: '报告',
        title: '系统配置报告',
        description: '系统配置报告',
        headers: ['状态', '权限描述', '权限编码', '权限经理', '申请工作流', '删除工作流', '重置密码工作流', '转部门工作流']
      },
      {
        path: '/report/variationReport',
        sectionKey: 'report',
        sectionLabel: '报告',
        title: '权限差异报告',
        description: '权限差异分析',
        buttons: ['生成报告', '导入权限列表'],
        headers: ['权限路径', '状态', '详情']
      }
    ]
  },
  {
    key: 'sysAccessManager',
    label: '系统/权限经理',
    entries: [
      {
        path: '/sysAccessManager/empAccessManagement',
        sectionKey: 'sysAccessManager',
        sectionLabel: '系统/权限经理',
        title: '系统用户管理',
        description: 'AMSAdmin 管理的系统/权限如下',
        buttons: ['申请'],
        headers: ['权限路径', '状态', '账号管理']
      },
      {
        path: '/sysAccessManager/reviewCheck',
        sectionKey: 'sysAccessManager',
        sectionLabel: '系统/权限经理',
        title: '审核查看',
        description: '系统/权限经理审核查看',
        tabs: ['进行中', '未开始'],
        headers: ['系统名称', '审核名称', '审核类型', '开始日期', '结束日期']
      }
    ]
  },
  {
    key: 'systemAdmin',
    label: '系统管理员',
    entries: [
      {
        path: '/systemAdmin/userAccessManagement',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '用户权限查看',
        description: '全局用户账号与权限视图',
        headers: ['前缀', '账号名称', '账号类型', '账号描述']
      },
      {
        path: '/systemAdmin/empAccessManagement',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '系统用户管理',
        description: '系统级用户权限管理',
        headers: ['权限路径', '状态', '文件管理', '账号管理']
      },
      {
        path: '/systemAdmin/copyProfile',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '用户权限复制',
        description: '复制用户权限/Profile',
        headers: ['参考用户', '目标用户', '权限路径']
      },
      {
        path: '/systemAdmin/sysAccessAdmin',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '系统/权限管理',
        description: '系统与权限配置管理',
        tabs: ['系统', '权限'],
        headers: ['DMS_TMS', 'LIMS', 'MES', 'WMS', '电子台账系统', '基础账号', '仪器设备']
      },
      {
        path: '/systemAdmin/reviewManagement',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '审核管理',
        description: '审核计划管理',
        headers: ['系统名称', '审核名称', '审核类型', '开始日期', '修改', '删除', '详情']
      },
      {
        path: '/systemAdmin/admindelegation',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '代理设置',
        description: '管理员代理设置',
        headers: ['用户名', '用户全称', '开始日期', '结束日期', '操作', '状态']
      },
      {
        path: '/systemAdmin/adminworkflow',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '工作流配置',
        description: '工作流与工作组配置',
        tabs: ['工作流', '工作组'],
        headers: ['工作流名称', '描述', '步骤数', '系统名称', '详情', '组名称']
      },
      {
        path: '/systemAdmin/applicationConfig',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '应用配置',
        description: '应用配置管理',
        buttons: ['删除', '修改', '添加'],
        headers: ['应用名称', '前缀']
      },
      {
        path: '/systemAdmin/mailTemplateCfg',
        sectionKey: 'systemAdmin',
        sectionLabel: '系统管理员',
        title: '邮件模板配置',
        description: '邮件模板配置',
        buttons: ['删除', '修改', '添加'],
        headers: ['模板名称', '描述', '主题', '详情']
      }
    ]
  },
  {
    key: 'hrAdmin',
    label: '人事信息管理员',
    entries: [
      {
        path: '/hrAdmin/hrManagement',
        sectionKey: 'hrAdmin',
        sectionLabel: '人事信息管理员',
        title: '人事信息管理',
        description: '当前所有员工人事信息如下',
        buttons: ['添加'],
        headers: ['员工编码', '员工姓名', '部门名称', '部门负责人', '状态', '修改信息', '修改部门', '详情']
      },
      {
        path: '/hrAdmin/enterprisemaintain',
        sectionKey: 'hrAdmin',
        sectionLabel: '人事信息管理员',
        title: '企业架构管理',
        description: '企业当前架构如下',
        buttons: ['添加'],
        headers: ['部门名称', '部门负责人', '状态', '部门成员', '修改', '删除', '详情']
      }
    ]
  }
]

export const targetCloneEntries = targetCloneSections.flatMap((section) => section.entries)

export function findTargetCloneEntry(path: string) {
  return targetCloneEntries.find((entry) => entry.path === path)
}

export function findTargetCloneSection(key: string) {
  return targetCloneSections.find((section) => section.key === key)
}
