<template>
  <section class="page">
    <h1>用户权限查询</h1>
    <p>后端用户权限接口尚未接入，当前为安全占位页，避免页面报错。</p>

    <table class="result-table">
      <thead>
        <tr>
          <th>用户</th>
          <th>角色</th>
          <th>设备账号</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>demo_user</td>
          <td>只读角色</td>
          <td>readonly_demo</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 12px;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
}

.result-table th,
.result-table td {
  padding: 8px;
  border: 1px solid #dcdfe6;
  text-align: left;
}
</style>
