<template>
  <section class="drawer-form">
    <header class="drawer-form__header">
      <div>
        <p class="drawer-form__eyebrow">{{ t('departments.drawer.eyebrow') }}</p>
        <h2 class="drawer-form__title">
          {{ mode === 'create' ? t('departments.drawer.titleCreate') : t('departments.drawer.titleEdit') }}
        </h2>
        <p class="drawer-form__subtitle">
          {{ mode === 'create'
            ? t('departments.drawer.subtitleCreate')
            : t('departments.drawer.subtitleEdit') }}
        </p>
      </div>
      <button class="drawer-form__close" type="button" :disabled="submitting" @click="$emit('close')">
        {{ t('common.close') }}
      </button>
    </header>

    <form class="drawer-form__body" @submit.prevent="handleSubmit">
      <label class="field" for="department-name">
        <span class="field__label">{{ t('departments.drawer.departmentName') }}</span>
        <input
          id="department-name"
          v-model="form.departmentName"
          class="field__control"
          name="departmentName"
          type="text"
          autocomplete="off"
          :disabled="submitting"
          required
        />
      </label>

      <label class="field" for="manager-user-id">
        <span class="field__label">{{ t('departments.drawer.manager') }}</span>
        <select
          id="manager-user-id"
          v-model="form.managerUserId"
          class="field__control"
          name="managerUserId"
          :disabled="submitting"
        >
          <option value="">{{ t('departments.drawer.managerUnassigned') }}</option>
          <option
            v-for="option in managerOptions"
            :key="option.id"
            :value="option.id"
          >
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field" for="department-description">
        <span class="field__label">{{ t('departments.drawer.description') }}</span>
        <textarea
          id="department-description"
          v-model="form.description"
          class="field__control field__control--textarea"
          name="description"
          rows="4"
          :disabled="submitting"
        />
      </label>

      <label class="field" for="department-status">
        <span class="field__label">{{ t('departments.drawer.status') }}</span>
        <select
          id="department-status"
          v-model="form.status"
          class="field__control"
          name="status"
          :disabled="submitting"
          required
        >
          <option value="ENABLED">{{ t('departments.status.enabled') }}</option>
          <option value="DISABLED">{{ t('departments.status.disabled') }}</option>
        </select>
      </label>

      <p v-if="errorMessage" class="drawer-form__error" role="alert">
        {{ errorMessage }}
      </p>

      <footer class="drawer-form__actions">
        <button class="button button--ghost" type="button" :disabled="submitting" @click="$emit('close')">
          {{ t('common.cancel') }}
        </button>
        <button class="button button--primary" type="submit" :disabled="submitting || !canSubmit">
          {{ submitting ? t('common.saving') : mode === 'create' ? t('departments.drawer.submitCreate') : t('departments.drawer.submitSave') }}
        </button>
      </footer>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18nText } from '../../i18n'
import type { DepartmentItem, DepartmentMutationPayload } from '../../api/departments'

interface DepartmentManagerOption {
  id: string
  label: string
}

interface FormState {
  departmentName: string
  managerUserId: string
  description: string
  status: string
}

const props = defineProps<{
  mode: 'create' | 'edit'
  initialDepartment?: DepartmentItem | null
  managerOptions: DepartmentManagerOption[]
  submitting?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  submit: [payload: DepartmentMutationPayload]
  close: []
}>()
const { t } = useI18nText()

const form = reactive<FormState>({
  departmentName: '',
  managerUserId: '',
  description: '',
  status: 'ENABLED'
})

function syncForm() {
  const currentManagerId = props.initialDepartment?.managerUserId ?? ''
  const hasAvailableManager =
    currentManagerId.length > 0 &&
    props.managerOptions.some((option) => option.id === currentManagerId)

  form.departmentName = props.initialDepartment?.departmentName ?? ''
  form.managerUserId = hasAvailableManager ? currentManagerId : ''
  form.description = props.initialDepartment?.description ?? ''
  form.status = props.initialDepartment?.status ?? 'ENABLED'
}

watch(
  () => [props.initialDepartment, props.managerOptions, props.mode],
  syncForm,
  { immediate: true }
)

const canSubmit = computed(() => form.departmentName.trim().length > 0 && form.status.trim().length > 0)

function handleSubmit() {
  if (!canSubmit.value) {
    return
  }

  emit('submit', {
    departmentName: form.departmentName.trim(),
    managerUserId: form.managerUserId.trim().length > 0 ? Number(form.managerUserId) : null,
    description: form.description.trim(),
    status: form.status.trim()
  })
}
</script>

<style scoped>
.drawer-form {
  height: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  background:
    radial-gradient(420px 260px at 14% 8%, rgba(94, 234, 212, 0.12), transparent 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(7, 10, 20, 0.08)),
    rgba(11, 16, 32, 0.94);
  color: var(--cockpit-text);
}

.drawer-form__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 22px 18px;
  border-bottom: 1px solid var(--cockpit-border);
}

.drawer-form__eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--cockpit-accent);
}

.drawer-form__title {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 900;
}

.drawer-form__subtitle {
  margin: 8px 0 0;
  color: var(--cockpit-muted);
  font-size: 13px;
  line-height: 1.5;
}

.drawer-form__close {
  min-height: 44px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.24);
  color: var(--cockpit-text);
  cursor: pointer;
}

.drawer-form__body {
  display: grid;
  align-content: start;
  gap: 16px;
  padding: 20px 22px 24px;
  overflow-y: auto;
}

.field {
  display: grid;
  gap: 8px;
}

.field__label {
  font-size: 13px;
  font-weight: 700;
}

.field__control {
  min-height: 46px;
  width: 100%;
  padding: 11px 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  color: var(--cockpit-text);
  font: inherit;
}

.field__control--textarea {
  min-height: 120px;
  resize: vertical;
}

.field__control:focus {
  outline: 2px solid rgba(94, 234, 212, 0.5);
  outline-offset: 2px;
  border-color: rgba(94, 234, 212, 0.38);
}

.field__control:disabled,
.drawer-form__close:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.drawer-form__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(251, 113, 133, 0.35);
  background: rgba(251, 113, 133, 0.14);
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  line-height: 1.5;
}

.drawer-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
}

.button {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid transparent;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.button--ghost {
  background: rgba(0, 0, 0, 0.18);
  border-color: rgba(255, 255, 255, 0.16);
  color: var(--cockpit-text);
}

.button--primary {
  background: linear-gradient(135deg, rgba(94, 234, 212, 0.95), rgba(96, 165, 250, 0.95));
  color: #041322;
}
</style>
