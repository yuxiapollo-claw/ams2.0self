<template>
  <section class="drawer-form">
    <header class="drawer-form__header">
      <div>
        <p class="drawer-form__eyebrow">{{ t('users.drawer.eyebrow') }}</p>
        <h2 class="drawer-form__title">
          {{ mode === 'create' ? t('users.drawer.titleCreate') : t('users.drawer.titleEdit') }}
        </h2>
        <p class="drawer-form__subtitle">
          {{ mode === 'create'
            ? t('users.drawer.subtitleCreate')
            : t('users.drawer.subtitleEdit') }}
        </p>
      </div>
      <button class="drawer-form__close" type="button" @click="$emit('close')">
        {{ t('common.close') }}
      </button>
    </header>

    <form class="drawer-form__body" @submit.prevent="handleSubmit">
      <label class="field" for="user-code">
        <span class="field__label">{{ t('users.drawer.userCode') }}</span>
        <input
          id="user-code"
          v-model="form.userCode"
          class="field__control"
          name="userCode"
          type="text"
          autocomplete="off"
          :disabled="submitting"
          required
        />
      </label>

      <label class="field" for="user-name">
        <span class="field__label">{{ t('users.drawer.userName') }}</span>
        <input
          id="user-name"
          v-model="form.userName"
          class="field__control"
          name="userName"
          type="text"
          autocomplete="off"
          :disabled="submitting"
          required
        />
      </label>

      <label class="field" for="department-id">
        <span class="field__label">{{ t('users.drawer.department') }}</span>
        <select
          id="department-id"
          v-model="form.departmentId"
          class="field__control"
          name="departmentId"
          :disabled="submitting"
          required
        >
          <option
            v-for="option in departmentOptions"
            :key="option.id"
            :value="option.id"
          >
            {{ option.departmentName }}
          </option>
        </select>
      </label>

      <label class="field" for="employment-status">
        <span class="field__label">{{ t('users.drawer.employmentStatus') }}</span>
        <select
          id="employment-status"
          v-model="form.employmentStatus"
          class="field__control"
          name="employmentStatus"
          :disabled="submitting"
          required
        >
          <option value="ACTIVE">{{ t('users.employment.active') }}</option>
          <option value="LEAVE">{{ t('users.employment.leave') }}</option>
          <option value="RESIGNED">{{ t('users.employment.resigned') }}</option>
        </select>
      </label>

      <label class="field" for="login-name">
        <span class="field__label">{{ t('users.drawer.loginName') }}</span>
        <input
          id="login-name"
          v-model="form.loginName"
          class="field__control"
          name="loginName"
          type="text"
          autocomplete="off"
          :disabled="submitting"
          required
        />
      </label>

      <label class="field" for="account-status">
        <span class="field__label">{{ t('users.drawer.accountStatus') }}</span>
        <select
          id="account-status"
          v-model="form.accountStatus"
          class="field__control"
          name="accountStatus"
          :disabled="submitting"
          required
        >
          <option value="ENABLED">{{ t('users.status.enabled') }}</option>
          <option value="DISABLED">{{ t('users.status.disabled') }}</option>
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
          {{ submitting ? t('common.saving') : mode === 'create' ? t('users.drawer.submitCreate') : t('users.drawer.submitSave') }}
        </button>
      </footer>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18nText } from '../../i18n'
import type { UserMutationPayload, UserItem } from '../../api/users'

interface DepartmentOption {
  id: string
  departmentName: string
}

interface FormState {
  userCode: string
  userName: string
  departmentId: string
  employmentStatus: string
  loginName: string
  accountStatus: string
}

const props = defineProps<{
  mode: 'create' | 'edit'
  initialUser?: UserItem | null
  departmentOptions: DepartmentOption[]
  submitting?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  submit: [payload: UserMutationPayload]
  close: []
}>()
const { t } = useI18nText()

const form = reactive<FormState>({
  userCode: '',
  userName: '',
  departmentId: '',
  employmentStatus: 'ACTIVE',
  loginName: '',
  accountStatus: 'ENABLED'
})

function syncForm() {
  form.userCode = props.initialUser?.userCode ?? ''
  form.userName = props.initialUser?.userName ?? ''
  form.departmentId = props.initialUser?.departmentId ?? props.departmentOptions[0]?.id ?? ''
  form.employmentStatus = props.initialUser?.employmentStatus ?? 'ACTIVE'
  form.loginName = props.initialUser?.loginName ?? ''
  form.accountStatus = props.initialUser?.accountStatus ?? 'ENABLED'
}

watch(
  () => [props.initialUser, props.departmentOptions, props.mode],
  syncForm,
  { immediate: true }
)

const canSubmit = computed(() =>
  [form.userCode, form.userName, form.departmentId, form.employmentStatus, form.loginName, form.accountStatus]
    .every((value) => value.trim().length > 0)
)

function handleSubmit() {
  if (!canSubmit.value) {
    return
  }

  emit('submit', {
    userCode: form.userCode.trim(),
    userName: form.userName.trim(),
    departmentId: Number(form.departmentId),
    employmentStatus: form.employmentStatus.trim(),
    loginName: form.loginName.trim(),
    accountStatus: form.accountStatus.trim()
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

.field__control:focus {
  outline: 2px solid rgba(94, 234, 212, 0.5);
  outline-offset: 2px;
  border-color: rgba(94, 234, 212, 0.38);
}

.field__control:disabled {
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
