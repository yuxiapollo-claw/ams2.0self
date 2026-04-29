<template>
  <section class="hero">
    <div class="hero__main">
      <div class="hero__kicker" v-if="kicker">{{ kicker }}</div>
      <h1 class="hero__title">{{ title }}</h1>
      <p class="hero__subtitle" v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div class="hero__actions" v-if="$slots.actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  kicker?: string
}>()
</script>

<style scoped>
.hero {
  padding: 18px 18px 14px;
  border: 1px solid var(--cockpit-border);
  border-radius: var(--cockpit-radius-lg);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: var(--cockpit-shadow);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.hero__kicker {
  font-size: 12px;
  color: var(--cockpit-muted);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.hero__title {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.hero__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--cockpit-muted);
}

.hero__actions {
  display: flex;
  gap: 10px;
}
</style>

