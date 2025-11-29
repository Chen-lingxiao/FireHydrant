/// <reference types="vite/client" />
// 用 import type 声明仅导入类型，并忽略未使用提示
import type { ElMessage } from 'element-plus' // eslint-disable-line @typescript-eslint/no-unused-vars

// 声明全局变量 ElMessage
declare global {
  const ElMessage: typeof ElMessage
  const ElMessageBox: typeof ElMessage
}

export {}
// 这样可以避免全局作用域污染
// 并确保类型声明仅在需要时生效
// 例如在 Vue 组件或其他 TypeScript 文件中
// 可以直接使用 ElMessage 而不会报错
// 例如： ElMessage.success('操作成功')
