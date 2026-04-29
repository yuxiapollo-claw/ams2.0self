import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { RouterView } from 'vue-router'
import router from './router'
import { usePreferencesStore } from './stores/preferences'
import './styles/theme.css'

// Used by unit smoke tests as a non-visual bootstrap marker.
// Keep it outside `#app` so we can mount RouterView directly.
if (typeof document !== 'undefined' && !document.getElementById('ams-root')) {
  const marker = document.createElement('div')
  marker.id = 'ams-root'
  marker.style.display = 'none'
  marker.textContent = 'AMS2.0'
  document.body.appendChild(marker)
}

const pinia = createPinia()
const app = createApp(RouterView)

app.use(pinia)
usePreferencesStore(pinia).initializePreferences()

app.use(router).use(ElementPlus).mount('#app')
