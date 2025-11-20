import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

// 定义用户信息类型
interface UserInfo {
  id: number
  name: string
  sex: string
  birthDate: string
  department: string
  telephone: string
  email: number
  role: string
  createTime: string
}

export const useUserStore = defineStore(
  'user',
  () => {
    const userInfo = ref<UserInfo | null>(null) // 用户信息状态
    // const token = ref<string | null>(null) // 用户token状态

    // 计算属性：从用户信息中获取权限信息
    const role = computed(() => {
      return userInfo.value?.role
    })
    // 计算属性：判断用户是否已登录
    const isLoggedIn = computed(() => {
      return !!userInfo.value // 如果userInfo存在，则表示已登录
    })
    // 设置用户信息
    const setUserInfo = (info: UserInfo) => {
      userInfo.value = info
    }
    // 清除用户信息（退出登录）
    const clearUserInfo = () => {
      userInfo.value = null
      // token.value = ''
    }
    return { userInfo, setUserInfo, isLoggedIn, role, clearUserInfo }
  },
  {
    persist: {
      // 移除token的持久化配置，仅持久化用户信息（非敏感数据）
      key: 'user-store',
      storage: localStorage,
    },
  },
)
