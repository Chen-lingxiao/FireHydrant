import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
// 全局样式
import '@/assets/main.scss'
// 引入Element Plus及其中文语言包
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
const app = createApp(App)

app.use(createPinia())
app.use(router)
// 注册Element Plus，并设置语言为中文
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')
