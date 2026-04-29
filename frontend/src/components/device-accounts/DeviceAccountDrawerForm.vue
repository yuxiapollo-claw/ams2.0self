<template>
  <section class="drawer-form">
    <header class="drawer-form__header">
      <div>
        <p class="drawer-form__eyebrow">{{ t('deviceAccounts.drawer.eyebrow') }}</p>
        <h2 class="drawer-form__title">
          {{ mode === 'create' ? t('deviceAccounts.drawer.titleCreate') : t('deviceAccounts.drawer.titleEdit') }}
        </h2>
        <p class="drawer-form__subtitle">
          {{ mode === 'create'
            ? t('deviceAccounts.drawer.subtitleCreate')
            : t('deviceAccounts.drawer.subtitleEdit') }}
        </p>
      </div>
      <button class="drawer-form__close" type="button" :disabled="submitting" @click="$emit('close')">
        {{ t('common.close') }}
      </button>
    </header>

    <form class="drawer-form__body" @submit.prevent="handleSubmit">
      <label class="field" for="device-node-id">
        <span class="field__label">{{ t('deviceAccounts.drawer.device') }}</span>
        <select
          id="device-node-id"
          v-model="form.deviceNodeId"
          class="field__control"
          name="deviceNodeId"
          :disabled="submitting"
          required
        >
          <option v-for="option in deviceOptions" :key="option.id" :value="option.id">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field" for="user-id">
        <span class="field__label">{{ t('deviceAccounts.drawer.boundUser') }}</span>
        <select
          id="user-id"
          v-model="form.userId"
          class="field__control"
          name="userId"
          :disabled="submitting"
        >
          <option value="">{{ t('deviceAccounts.drawer.leaveUnbound') }}</option>
          <option v-for="option in userOptions" :key="option.id" :value="option.id">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field" for="account-name">
        <span class="field__label">{{ t('deviceAccounts.drawer.accountName') }}</span>
        <input
          id="account-name"
          v-model="form.accountName"
          class="field__control"
          name="accountName"
          type="text"
          autocomplete="off"
          :disabled="submitting"
          required
        />
      </label>

      <label class="field" for="account-status">
        <span class="field__label">{{ t('deviceAccounts.drawer.accountStatus') }}</span>
        <select
          id="account-status"
          v-model="form.accountStatus"
          class="field__control"
          name="accountStatus"
          :disabled="submitting"
          required
        >
          <option value="ENABLED">{{ t('deviceAccounts.status.enabled') }}</option>
          <option value="DISABLED">{{ t('deviceAccounts.status.disabled') }}</option>
        </select>
      </label>

      <label class="field" for="source-type">
        <span class="field__label">{{ t('deviceAccounts.drawer.sourceType') }}</span>
        <select
          id="source-type"
          v-model="form.sourceType"
          class="field__control"
          name="sourceType"
          :disabled="submitting"
          required
        >
          <option value="MANUAL">{{ t('deviceAccounts.source.manual') }}</option>
          <option value="IMPORTED">{{ t('deviceAccounts.source.imported') }}</option>
        </select>
      </label>

      <label class="field" for="device-account-remark">
        <span class="field__label">{{ t('deviceAccounts.drawer.remark') }}</span>
        <textarea
          id="device-account-remark"
          v-model="form.remark"
          class="field__control field__control--textarea"
          name="remark"
          rows="4"
          :disabled="submitting"
        />
      </label>

      <p v-if="errorMessage" class="drawer-form__error" role="alert">
        {{ errorMessage }}
      </p>

      <footer class="drawer-form__actions">
        <button class="button button--ghost" type="button" :disabled="submitting" @click="$emit('close')">
          {{ t('common.cancel') }}
        </button>
        <button class="button button--primary" type="submit" :disabled="submitting || !canSubmit">
          {{ submitting ? t('common.saving') : mode === 'create' ? t('deviceAccounts.drawer.submitCreate') : t('deviceAccounts.drawer.submitSave') }}
        </button>
      </footer>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18nText } from '../../i18n'
import type { DeviceAccountItem, DeviceAccountMutationPayload } from '../../api/device-accounts'

interface SelectOption {
  id: string
  label: string
}

interface FormState {
  deviceNodeId: string
  userId: string
  accountName: string
  accountStatus: string
  sourceType: string
  remark: string
}

const props = defineProps<{
  mode: 'create' | 'edit'
  initialDeviceAccount?: DeviceAccountItem | null
  deviceOptions: SelectOption[]
  userOptions: SelectOption[]
  submitting?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  submit: [payload: DeviceAccountMutationPayload]
  close: []
}>()
const { t } = useI18nText()

const form = reactive<FormState>({
  deviceNodeId: '',
  userId: '',
  accountName: '',
  accountStatus: 'ENABLED',
  sourceType: 'MANUAL',
  remark: ''
})

function syncForm() {
  form.deviceNodeId = String(props.initialDeviceAccount?.deviceNodeId ?? props.deviceOptions[0]?.id ?? '')
  form.userId = props.initialDeviceAccount?.userId ?? ''
  form.accountName = props.initialDeviceAccount?.accountName ?? ''
  form.accountStatus = props.initialDeviceAccount?.accountStatus ?? 'ENABLED'
  form.sourceType = props.initialDeviceAccount?.sourceType ?? 'MANUAL'
  form.remark = props.initialDeviceAccount?.remark ?? ''
}

watch(
  () => [props.initialDeviceAccount, props.deviceOptions, props.mode],
  syncForm,
  { immediate: true }
)

const canSubmit = computed(() =>
  [form.deviceNodeId, form.accountName, form.accountStatus, form.sourceType]
    .every((value) => value.trim().length > 0)
)

function handleSubmit() {
  if (!canSubmit.value) {
    return
  }

  emit('submit', {
    deviceNodeId: Number(form.deviceNodeId),
    userId: form.userId.trim().length > 0 ? Number(form.userId) : null,
    accountName: form.accountName.trim(),
    accountStatus: form.accountStatus.trim(),
    sourceType: form.sourceType.trim(),
    remark: form.remark.trim()
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
