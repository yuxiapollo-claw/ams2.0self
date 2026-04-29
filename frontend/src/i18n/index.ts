import { computed } from 'vue'
import { DEFAULT_LOCALE, usePreferencesStore, type Locale } from '../stores/preferences'
import { messages, type MessageKey } from './messages'

export type { MessageKey } from './messages'

export function translate(locale: Locale, key: MessageKey): string {
  return messages[locale]?.[key] ?? messages[DEFAULT_LOCALE][key] ?? key
}

export function useI18nText() {
  const preferences = usePreferencesStore()

  return {
    t: (key: MessageKey) => translate(preferences.locale, key),
    locale: computed(() => preferences.locale),
    theme: computed(() => preferences.theme)
  }
}
