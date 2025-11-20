import axios from 'axios'
// import { useUserStore } from '@/stores/index'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: 'http://localhost:8080', // 设置基础URL
  timeout: 5000, // 设置超时时间
  headers: {
    'Content-Type': 'application/json;charset=utf-8', // 设置请求头
  },
  withCredentials: true, // 核心：允许跨域请求携带Cookie
})
// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 在发送请求之前做什么
    // const userStore = useUserStore()
    // if (userStore.token) {
    //   // 如果有token，则设置请求头
    //   config.headers.Authorization = `Bearer ${userStore.token}`
    // }
    return config
  },
  (error) => {
    // 处理请求错误
    ElMessage.error('请求错误，请稍后再试')
    return Promise.reject(error)
  },
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 对响应数据做些什么
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败，请稍后再试')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    // 处理响应错误
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      // const userStore = useUserStore()
      // userStore.setToken('') // 清空token
      // 可选：跳转到登录页
      router.push('/')
    }
    ElMessage.error('服务器响应错误')
    return Promise.reject(error)
  },
)

export default request // 导出请求实例
