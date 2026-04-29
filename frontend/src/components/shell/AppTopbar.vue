<template>
  <header class="topbar">
    <nav class="topbar__primary" aria-label="一级导航">
      <RouterLink
        v-for="section in targetCloneSections"
        :key="section.key"
        :to="section.entries[0].path"
        class="topbar__link"
        :class="{ 'is-active': currentSectionKey === section.key }"
      >
        {{ section.label }}
      </RouterLink>
    </nav>

    <div class="topbar__meta">
      <span class="topbar__user">AMSAdmin</span>
      <button class="topbar__locale" type="button" @click="preferences.setLocale('zh-CN')">中文</button>
      <button class="topbar__locale" type="button" @click="preferences.setLocale('en-US')">English</button>
      <span class="topbar__version">版本: 1.0.0</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { targetCloneSections } from '../../config/target-clone'
import { usePreferencesStore } from '../../stores/preferences'

const route = useRoute()
const preferences = usePreferencesStore()

const currentSectionKey = computed(() =>
  typeof route.meta.sectionKey === 'string' ? route.meta.sectionKey : 'dashboard'
)
</script>

<style scoped>
.topbar {
  height: 56px;
  border-bottom: 1px solid #dcdfe6;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.topbar__primary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar__link {
  color: #303133;
  text-decoration: none;
  font-size: 14px;
  line-height: 20px;
  padding: 6px 0;
}

.topbar__link.is-active {
  color: #409eff;
}

.topbar__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #606266;
}

.topbar__locale {
  border: none;
  background: transparent;
  padding: 0;
  color: inherit;
  cursor: pointer;
  font-size: 12px;
}

.topbar__user,
.topbar__version {
  white-space: nowrap;
}
</style>
