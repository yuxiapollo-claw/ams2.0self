import { defineStore } from 'pinia'
import { ref } from 'vue'

export type Locale = 'zh-CN' | 'en-US'
export type ThemeMode = 'dark' | 'light'

const LOCALE_KEY = 'ams_locale'
const THEME_KEY = 'ams_theme'
export const PREFERENCE_STORAGE_KEYS = {
  locale: LOCALE_KEY,
  theme: THEME_KEY
} as const

export const DEFAULT_LOCALE: Locale = 'zh-CN'
export const DEFAULT_THEME: ThemeMode = 'dark'

function isLocale(value: string | null): value is Locale {
  return value === 'zh-CN' || value === 'en-US'
}

function isTheme(value: string | null): value is ThemeMode {
  return value === 'dark' || value === 'light'
}

function applyTheme(theme: ThemeMode) {
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.theme = theme
  }
}

function readStoredValue(key: string) {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

function writeStoredValue(key: string, value: string) {
  if (typeof window === 'undefined') {
    return
  }

  try {
    window.localStorage.setItem(key, value)
  } catch {
    // Ignore storage failures so preference bootstrap never blocks the app shell.
  }
}

function persist(locale: Locale, theme: ThemeMode) {
  writeStoredValue(LOCALE_KEY, locale)
  writeStoredValue(THEME_KEY, theme)
}

export const usePreferencesStore = defineStore('preferences', () => {
  const locale = ref<Locale>(DEFAULT_LOCALE)
  const theme = ref<ThemeMode>(DEFAULT_THEME)

  function initializePreferences() {
    const savedLocale = readStoredValue(LOCALE_KEY)
    const savedTheme = readStoredValue(THEME_KEY)

    locale.value = isLocale(savedLocale) ? savedLocale : DEFAULT_LOCALE
    theme.value = isTheme(savedTheme) ? savedTheme : DEFAULT_THEME

    persist(locale.value, theme.value)
    applyTheme(theme.value)
  }

  function setLocale(nextLocale: Locale) {
    locale.value = nextLocale
    persist(locale.value, theme.value)
  }

  function toggleLocale() {
    setLocale(locale.value === 'zh-CN' ? 'en-US' : 'zh-CN')
  }

  function setTheme(nextTheme: ThemeMode) {
    theme.value = nextTheme
    persist(locale.value, theme.value)
    applyTheme(theme.value)
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return {
    locale,
    theme,
    initializePreferences,
    setLocale,
    toggleLocale,
    setTheme,
    toggleTheme
  }
})
