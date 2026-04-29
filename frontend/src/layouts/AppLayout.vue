<template>
  <div class="layout-shell">
    <AppTopbar />
    <div class="layout-shell__body">
      <AppSidebar />
      <main class="layout-shell__content" role="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from '../components/shell/AppSidebar.vue'
import AppTopbar from '../components/shell/AppTopbar.vue'
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  background: #f5f7fa;
}

.layout-shell__body {
  min-height: calc(100vh - 56px);
  display: grid;
  grid-template-columns: 220px 1fr;
}

.layout-shell__content {
  padding: 16px;
  min-width: 0;
}
</style>
