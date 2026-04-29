<template>
  <section class="dashboard-page">
    <header class="dashboard-page__hero">
      <h1>你好, AMSAdmin</h1>
      <h2>欢迎使用权限通</h2>
      <p>您可以通过左侧的菜单栏来管理您的账号，权限及任务。或者，点击首页的“待办事项”，直接跳转到对应页面。</p>
    </header>

    <div class="dashboard-page__grid">
      <section class="dashboard-card dashboard-card--wide">
        <h3>系统公告</h3>
        <div class="dashboard-card__body">h</div>
      </section>
      <section class="dashboard-card dashboard-card--wide">
        <h3>待办事项</h3>
        <div class="dashboard-card__body">0</div>
      </section>
      <section class="dashboard-card">
        <h3>待审批</h3>
        <div class="dashboard-card__body">0</div>
      </section>
      <section class="dashboard-card">
        <h3>待操作</h3>
        <div class="dashboard-card__body">0</div>
      </section>
      <section class="dashboard-card">
        <h3>待审核</h3>
        <div class="dashboard-card__body">0</div>
      </section>
      <section class="dashboard-card">
        <h3>待申诉</h3>
        <div class="dashboard-card__body">0</div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 16px;
}

.dashboard-page__hero {
  background: #fff;
  border: 1px solid #dcdfe6;
  padding: 16px;
}

.dashboard-page__hero h1,
.dashboard-page__hero h2,
.dashboard-page__hero p {
  margin: 0;
}

.dashboard-page__hero h2 {
  margin-top: 8px;
  font-size: 20px;
  color: #303133;
}

.dashboard-page__hero p {
  margin-top: 8px;
  color: #606266;
  line-height: 1.6;
}

.dashboard-page__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-card {
  background: #fff;
  border: 1px solid #dcdfe6;
  padding: 16px;
}

.dashboard-card--wide {
  grid-column: span 2;
}

.dashboard-card h3,
.dashboard-card__body {
  margin: 0;
}

.dashboard-card__body {
  margin-top: 12px;
  color: #606266;
}
</style>
