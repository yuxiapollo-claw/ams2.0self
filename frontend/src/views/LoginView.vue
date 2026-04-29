<template>
  <section class="login-page">
    <div class="login-page__panel">
      <div class="login-page__header">
        <div>
          <div class="login-page__eyebrow">{{ copy.eyebrow }}</div>
          <h1 class="login-page__title">{{ copy.title }}</h1>
          <p class="login-page__subtitle">{{ copy.subtitle }}</p>
        </div>
        <div class="login-page__switches">
          <button class="header-chip" type="button" @click="preferences.toggleLocale()">
            {{ localeLabel }}
          </button>
          <button class="header-chip" type="button" @click="preferences.toggleTheme()">
            {{ themeLabel }}
          </button>
        </div>
      </div>

      <div class="login-page__content">
        <article class="login-page__highlight">
          <div class="highlight-card">
            <div class="highlight-card__label">{{ copy.highlight1Label }}</div>
            <div class="highlight-card__value">{{ copy.highlight1Value }}</div>
            <div class="highlight-card__hint">{{ copy.highlight1Hint }}</div>
          </div>
          <div class="highlight-card">
            <div class="highlight-card__label">{{ copy.highlight2Label }}</div>
            <div class="highlight-card__value">{{ copy.highlight2Value }}</div>
            <div class="highlight-card__hint">{{ copy.highlight2Hint }}</div>
          </div>
          <div class="highlight-card">
            <div class="highlight-card__label">{{ copy.highlight3Label }}</div>
            <div class="highlight-card__value">{{ copy.highlight3Value }}</div>
            <div class="highlight-card__hint">{{ copy.highlight3Hint }}</div>
          </div>
        </article>

        <article class="login-card">
          <form class="login-card__form" @submit.prevent="handleSubmit">
            <label class="field">
              <span>{{ copy.fields.loginName }}</span>
              <input
                v-model="loginName"
                name="loginName"
                type="text"
                autocomplete="username"
                :placeholder="copy.placeholders.loginName"
                :disabled="pending"
              />
            </label>

            <label class="field">
              <span>{{ copy.fields.password }}</span>
              <input
                v-model="password"
                name="password"
                type="password"
                autocomplete="current-password"
                :placeholder="copy.placeholders.password"
                :disabled="pending"
              />
            </label>

            <p v-if="errorMessage" class="login-card__error" role="alert">{{ errorMessage }}</p>

            <button class="login-card__submit" type="submit" :disabled="pending">
              {{ pending ? copy.actions.submitting : copy.actions.submit }}
            </button>
          </form>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18nText } from '../i18n'
import { useAuthStore } from '../stores/auth'
import { usePreferencesStore } from '../stores/preferences'

const router = useRouter()
const authStore = useAuthStore()
const preferences = usePreferencesStore()
const { locale, theme } = useI18nText()

const loginName = ref('')
const password = ref('')
const pending = ref(false)
const errorMessage = ref('')

const zhCopy = {
  eyebrow: 'AMS 权限管理',
  title: '本地权限治理工作台',
  subtitle: '统一维护用户、部门、系统、权限树与三类申请流程。',
  highlight1Label: '用户与组织',
  highlight1Value: '集中维护',
  highlight1Hint: '用户、部门、负责人和归属关系在一个入口统一处理。',
  highlight2Label: '系统与权限',
  highlight2Value: '五级树形',
  highlight2Hint: '系统是第一层，权限节点最多支持五层扩展。',
  highlight3Label: '申请流程',
  highlight3Value: '三种模式',
  highlight3Hint: '支持权限申请、权限删除和密码重置流程。',
  fields: {
    loginName: '登录名',
    password: '密码'
  },
  placeholders: {
    loginName: '请输入登录名',
    password: '请输入密码'
  },
  actions: {
    submit: '进入系统',
    submitting: '登录中...'
  },
  errors: {
    invalid: '登录名或密码不正确',
    fallback: '登录失败，请稍后重试。'
  },
  localeZh: '切换为 English',
  localeEn: '切换为中文',
  themeDark: '切换明亮',
  themeLight: '切换暗黑'
} as const

const enCopy = {
  eyebrow: 'AMS Access Control',
  title: 'Local Access Governance Workspace',
  subtitle: 'Manage users, departments, systems, permission trees, and the three request flows from one entry point.',
  highlight1Label: 'Users & Org',
  highlight1Value: 'Centralized',
  highlight1Hint: 'Manage users, departments, leaders, and ownership links in one place.',
  highlight2Label: 'Systems & Access',
  highlight2Value: 'Five Levels',
  highlight2Hint: 'The system name anchors level one, and permission nodes extend beneath it.',
  highlight3Label: 'Request Flow',
  highlight3Value: 'Three Modes',
  highlight3Hint: 'Support apply, remove, and password reset requests.',
  fields: {
    loginName: 'Login Name',
    password: 'Password'
  },
  placeholders: {
    loginName: 'Enter login name',
    password: 'Enter password'
  },
  actions: {
    submit: 'Enter Workspace',
    submitting: 'Signing in...'
  },
  errors: {
    invalid: 'The login name or password is incorrect',
    fallback: 'Sign in failed. Please try again later.'
  },
  localeZh: 'Switch to English',
  localeEn: 'Switch to Chinese',
  themeDark: 'Light Mode',
  themeLight: 'Dark Mode'
} as const

const copy = computed(() => (locale.value === 'zh-CN' ? zhCopy : enCopy))
const localeLabel = computed(() => (locale.value === 'zh-CN' ? copy.value.localeZh : copy.value.localeEn))
const themeLabel = computed(() => (theme.value === 'dark' ? copy.value.themeDark : copy.value.themeLight))

function extractHttpStatus(error: unknown): number | null {
  if (typeof error !== 'object' || error === null) {
    return null
  }

  const response = (error as { response?: unknown }).response
  if (typeof response !== 'object' || response === null) {
    return null
  }

  const status = (response as { status?: unknown }).status
  return typeof status === 'number' ? status : null
}

async function handleSubmit() {
  if (pending.value) {
    return
  }

  errorMessage.value = ''
  pending.value = true

  try {
    await authStore.login(loginName.value.trim(), password.value)
    await router.push('/dashboard')
  } catch (error) {
    errorMessage.value =
      extractHttpStatus(error) === 401 ? copy.value.errors.invalid : copy.value.errors.fallback
  } finally {
    pending.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 32px;
  display: grid;
  place-items: center;
}

.login-page__panel {
  width: min(1180px, 100%);
  padding: 28px;
  border-radius: 28px;
  border: 1px solid var(--cockpit-border);
  background:
    radial-gradient(540px 280px at 10% 12%, rgba(94, 234, 212, 0.16), transparent 58%),
    radial-gradient(520px 320px at 92% 12%, rgba(96, 165, 250, 0.14), transparent 58%),
    rgba(255, 255, 255, 0.05);
  box-shadow: var(--cockpit-shadow);
}

.login-page__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.login-page__eyebrow {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--cockpit-muted);
}

.login-page__title {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.08;
}

.login-page__subtitle {
  margin: 12px 0 0;
  max-width: 720px;
  color: var(--cockpit-muted);
  line-height: 1.65;
}

.login-page__switches {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.header-chip {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--cockpit-border);
  background: rgba(255, 255, 255, 0.08);
  color: var(--cockpit-text);
  font: inherit;
  cursor: pointer;
}

.login-page__content {
  margin-top: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr);
  gap: 20px;
}

.login-page__highlight {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.highlight-card,
.login-card {
  border: 1px solid var(--cockpit-border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.08);
}

.highlight-card {
  padding: 18px;
}

.highlight-card__label {
  font-size: 12px;
  color: var(--cockpit-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.highlight-card__value {
  margin-top: 12px;
  font-size: 26px;
  font-weight: 900;
}

.highlight-card__hint {
  margin-top: 12px;
  color: var(--cockpit-muted);
  line-height: 1.6;
  font-size: 13px;
}

.login-card {
  padding: 22px;
}

.login-card__form {
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
}

.field input {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 16px;
  border: 1px solid var(--cockpit-border);
  background: var(--cockpit-input-bg);
  color: var(--cockpit-text);
  outline: none;
  font: inherit;
}

.field input::placeholder {
  color: var(--cockpit-muted);
}

.login-card__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  color: rgba(255, 196, 206, 0.98);
  background: rgba(251, 113, 133, 0.14);
  border: 1px solid rgba(251, 113, 133, 0.32);
}

.login-card__submit {
  min-height: 48px;
  border-radius: 999px;
  border: 0;
  background: linear-gradient(135deg, var(--cockpit-accent), var(--cockpit-accent-2));
  color: #041322;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.login-card__submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1080px) {
  .login-page__content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .login-page {
    padding: 18px;
  }

  .login-page__panel {
    padding: 20px;
    border-radius: 22px;
  }

  .login-page__header,
  .login-page__highlight {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .login-page__title {
    font-size: 28px;
  }
}
</style>
