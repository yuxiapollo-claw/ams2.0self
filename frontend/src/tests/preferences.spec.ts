import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { translate, useI18nText } from '../i18n'
import { PREFERENCE_STORAGE_KEYS, usePreferencesStore } from '../stores/preferences'

describe('preferences store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    setActivePinia(createPinia())
  })

  it('initializes zh-CN + dark by default and persists toggles', () => {
    const store = usePreferencesStore()

    store.initializePreferences()

    expect(store.locale).toBe('zh-CN')
    expect(store.theme).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(translate(store.locale, 'shell.topbar.logout')).toBe('退出登录')

    store.toggleLocale()
    store.toggleTheme()

    expect(store.locale).toBe('en-US')
    expect(store.theme).toBe('light')
    expect(localStorage.getItem(PREFERENCE_STORAGE_KEYS.locale)).toBe('en-US')
    expect(localStorage.getItem(PREFERENCE_STORAGE_KEYS.theme)).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(translate(store.locale, 'shell.topbar.logout')).toBe('Sign Out')
  })

  it('restores saved values before first render', () => {
    localStorage.setItem(PREFERENCE_STORAGE_KEYS.locale, 'en-US')
    localStorage.setItem(PREFERENCE_STORAGE_KEYS.theme, 'light')

    const store = usePreferencesStore()
    store.initializePreferences()

    expect(store.locale).toBe('en-US')
    expect(store.theme).toBe('light')
    expect(translate(store.locale, 'shell.topbar.theme.light')).toBe('Light')
  })

  it('supports explicit setters and i18n helpers for shell keys', () => {
    const store = usePreferencesStore()
    store.initializePreferences()

    store.setLocale('en-US')
    store.setTheme('light')

    const { t, locale, theme } = useI18nText()

    expect(store.locale).toBe('en-US')
    expect(store.theme).toBe('light')
    expect(localStorage.getItem(PREFERENCE_STORAGE_KEYS.locale)).toBe('en-US')
    expect(localStorage.getItem(PREFERENCE_STORAGE_KEYS.theme)).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(locale.value).toBe('en-US')
    expect(theme.value).toBe('light')
    expect(t('shell.topbar.theme.dark')).toBe('Dark')
    expect(t('shell.topbar.locale.currentZh')).toBe('Chinese')
    expect(t('shell.topbar.locale.currentEn')).toBe('English')

    store.setLocale('zh-CN')
    store.setTheme('dark')

    expect(t('shell.topbar.theme.dark')).toBe('暗黑')
    expect(t('shell.topbar.locale.currentZh')).toBe('中文')
    expect(t('shell.topbar.locale.currentEn')).toBe('英文')
  })
})
