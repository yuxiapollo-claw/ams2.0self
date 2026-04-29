<template>
  <section class="target-page">
    <header class="target-page__header">
      <h1>{{ entry?.title ?? '页面' }}</h1>
      <p>{{ entry?.description ?? '页面建设中' }}</p>
    </header>

    <div v-if="entry?.buttons?.length" class="target-page__actions">
      <button v-for="button in entry.buttons" :key="button" type="button">
        {{ button }}
      </button>
    </div>

    <div v-if="entry?.tabs?.length" class="target-page__tabs">
      <button v-for="tab in entry.tabs" :key="tab" type="button">
        {{ tab }}
      </button>
    </div>

    <table v-if="entry?.headers?.length" class="target-page__table">
      <thead>
        <tr>
          <th v-for="header in entry.headers" :key="header">{{ header }}</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td :colspan="entry.headers.length">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { findTargetCloneEntry } from '../config/target-clone'

const route = useRoute()

const entry = computed(() => {
  const pagePath = typeof route.meta.pagePath === 'string' ? route.meta.pagePath : route.path
  return findTargetCloneEntry(pagePath)
})
</script>

<style scoped>
.target-page {
  min-height: 100%;
  display: grid;
  gap: 16px;
}

.target-page__header {
  background: #fff;
  border: 1px solid #dcdfe6;
  padding: 16px;
}

.target-page__header h1 {
  margin: 0;
  font-size: 24px;
}

.target-page__header p {
  margin: 8px 0 0;
  color: #606266;
}

.target-page__actions,
.target-page__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.target-page__actions button,
.target-page__tabs button {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  background: #fff;
  cursor: pointer;
}

.target-page__table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

.target-page__table th,
.target-page__table td {
  border: 1px solid #ebeef5;
  padding: 10px 12px;
  text-align: left;
  font-size: 14px;
}
</style>
