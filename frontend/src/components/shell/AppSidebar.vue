<template>
  <aside class="sidebar">
    <nav class="sidebar__nav" aria-label="侧边导航">
      <RouterLink
        v-for="entry in activeSection?.entries ?? []"
        :key="entry.path"
        :to="entry.path"
        class="sidebar__link"
        :class="{ 'is-active': route.path === entry.path }"
      >
        {{ entry.title }}
      </RouterLink>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { findTargetCloneSection } from '../../config/target-clone'

const route = useRoute()

const activeSection = computed(() => {
  const sectionKey = typeof route.meta.sectionKey === 'string' ? route.meta.sectionKey : 'dashboard'
  return findTargetCloneSection(sectionKey)
})
</script>

<style scoped>
.sidebar {
  width: 220px;
  border-right: 1px solid #dcdfe6;
  background: #fff;
  padding: 12px 0;
}

.sidebar__nav {
  display: flex;
  flex-direction: column;
}

.sidebar__link {
  padding: 10px 16px;
  color: #303133;
  text-decoration: none;
  font-size: 14px;
  line-height: 20px;
}

.sidebar__link:hover {
  background: #f5f7fa;
}

.sidebar__link.is-active {
  color: #409eff;
  background: #ecf5ff;
}
</style>
