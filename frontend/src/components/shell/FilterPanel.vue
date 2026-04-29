<template>
  <section class="panel">
    <header v-if="title || $slots.header" class="panel__header">
      <div v-if="title" class="panel__title">{{ title }}</div>
      <slot name="header" />
      <button
        v-if="collapsible"
        class="panel__toggle"
        type="button"
        @click="isOpen = !isOpen"
      >
        {{ isOpen ? t('shell.filter.collapse') : t('shell.filter.expand') }}
      </button>
    </header>
    <div v-show="!collapsible || isOpen" class="panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18nText } from '../../i18n'

const props = defineProps<{
  title?: string
  collapsible?: boolean
  defaultOpen?: boolean
}>()

const { t } = useI18nText()
const isOpen = ref(props.defaultOpen ?? true)
</script>

<style scoped>
.panel {
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: var(--cockpit-radius-lg);
  background: rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.panel__title {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.panel__toggle {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.12);
  color: var(--cockpit-text);
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 10px;
  cursor: pointer;
}

.panel__body {
  padding: 14px;
}
</style>
