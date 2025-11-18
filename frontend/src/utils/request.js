import axios from 'axios'
const request = axios.create({
  baseURL: '/api', // 设置基础URL
  timeout: 5000, // 设置超时时间
  headers: {
    'Content-Type': 'application/json;charset=utf-8', // 设置请求头
  },
})
// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 在发送请求之前做什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // 处理请求错误
    ElMessage.error('请求错误')
    return Promise.reject(error)
  },
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 对响应数据做些什么
    return response.data
  },
  (error) => {
    // 处理响应错误
    ElMessage.error('响应错误')
    return Promise.reject(error)
  },
)

export default request // 导出请求实例
