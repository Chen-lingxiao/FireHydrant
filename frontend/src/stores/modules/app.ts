import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  // 状态变量
  const count = ref(0)
  // 计算属性
  const doubleCount = computed(() => count.value * 2)
  // 方法
  function increment() {
    count.value++
  }

  return { count, doubleCount, increment }
})
