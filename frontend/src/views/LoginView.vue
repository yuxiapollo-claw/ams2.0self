<template>
  <section class="login-page">
    <div class="login-card">
      <div class="login-card__brand">LocalIAMSolution</div>
      <h1 class="login-card__title">权限管理系统</h1>

      <form class="login-card__form" @submit.prevent="handleSubmit">
        <p v-if="errorMessage" class="login-card__error" role="alert">{{ errorMessage }}</p>

        <label class="login-card__field">
          <span>用户名</span>
          <input
            v-model="loginName"
            name="loginName"
            type="text"
            autocomplete="username"
            :disabled="pending"
          />
        </label>

        <label class="login-card__field">
          <span>密码</span>
          <input
            v-model="password"
            name="password"
            type="password"
            autocomplete="current-password"
            :disabled="pending"
          />
        </label>

        <button class="login-card__submit" type="submit" :disabled="pending">
          登录
        </button>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loginName = ref('')
const password = ref('')
const pending = ref(false)
const errorMessage = ref('')

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
    await authStore.login(loginName.value, password.value)
    await router.push('/')
  } catch (error) {
    errorMessage.value =
      extractHttpStatus(error) === 401 ? '用户名或密码不正确' : '登录失败，请稍后重试'
  } finally {
    pending.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 88px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 360px;
  padding: 32px 28px;
  background: #fff;
  border: 1px solid #dcdfe6;
}

.login-card__brand {
  font-size: 14px;
  color: #909399;
  text-align: center;
}

.login-card__title {
  margin: 12px 0 24px;
  font-size: 24px;
  font-weight: 500;
  text-align: center;
  color: #303133;
}

.login-card__form {
  display: grid;
  gap: 16px;
}

.login-card__error {
  margin: 0;
  color: #f56c6c;
  font-size: 12px;
}

.login-card__field {
  display: grid;
  gap: 6px;
  color: #606266;
  font-size: 14px;
}

.login-card__field input {
  height: 36px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  outline: none;
}

.login-card__submit {
  height: 36px;
  border: 1px solid #409eff;
  background: #409eff;
  color: #fff;
  cursor: pointer;
}

.login-card__submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
</style>
