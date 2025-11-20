import { createPinia } from 'pinia'
import persist from 'pinia-plugin-persistedstate'
const pinia = createPinia()
pinia.use(persist) // 使用数据持久化插件
export default pinia // 导出pinia实例

export * from './modules/userStore' // 用户模块
export * from './modules/app' // 应用模块
