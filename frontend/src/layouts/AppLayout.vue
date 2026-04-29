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
  background: transparent;
}

.layout-shell__body {
  min-height: calc(100vh - var(--cockpit-topbar-h));
  display: grid;
  grid-template-columns: 308px 1fr;
}

.layout-shell__content {
  padding: 22px;
  min-width: 0;
}

@media (max-width: 980px) {
  .layout-shell__body {
    grid-template-columns: 1fr;
  }

  .layout-shell__content {
    padding: 18px;
  }
}
</style>
